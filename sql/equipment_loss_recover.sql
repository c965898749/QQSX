-- =====================================================================================
-- 装备丢失找回脚本 v2（按 uuid 链补齐/重挂，equipment_v2_migration 事故修复）
-- 执行库：master（游戏主库 yimem），需 MySQL 8.0+
-- 范围：只保证 eq_card（配置）与 eq_characters（玩家持有）不缺关联；其他表一律不动。
--
-- 思路（不依赖迁移当时跑到哪一步，全部靠 uuid 确定性推导）：
--   ① 配置补齐：eq_card 自增主键 uuid 与 eq_card_bak_v2 一一对应（如 uuid=160 被折叠删除）。
--      把现表缺失的 uuid 行从备份补回，id 按 equip_id_migration 调整成新格式；
--      若该新 id 已被组内保留行占用（折叠归并），则跳过不补，由持有侧改挂到保留行。
--   ② 持有整行补齐：eq_characters_bak_v2 同样按 uuid 一一对应，现表缺失的 uuid 行原样补回
--      （补回的是老 id，后续由重挂链自动改到现表真实存在的配置 id）。
--   ③ 持有重挂：eq_characters 靠自身 uuid 认装备实例，沿
--      持有行uuid -> 备份老id -> 备份配置行uuid -> 现表配置行当前id 的链重挂；
--      配置行被折叠掉的再走 映射表(老id->new_id) 挂到保留行；
--      老式尾号孤儿（如 P018_FK505）去尾号按 base 的映射挂。
--   所有 UPDATE 仅作用于"当前悬空"的行，重复执行安全（幂等），正常行不动。
--
-- ⚠ 先跑 A 诊断贴回结果，再执行 B，最后 C 验证。装备业务全部实时查库（EQ_CARD_CACHE 仅 stats 接口用），
--   无需重启后端，玩家重登刷新前端缓存即可生效。
--
-- 依赖表（1区/2区通用）：eq_card_bak_v2、eq_characters_bak_v2、equip_id_migration。
-- 区差异：1区跑过孤儿补全（有 eq_card_bak_orphan，尾号孤儿曾入 eq_card/映射表，B4 可达）；
--         2区没跑过（尾号孤儿从未进映射表，只能靠 B5/B6 去尾号 base 链重挂；
--         base 也不在映射表的落进 D 段人工清单，处理见 D 后注释 B8）。
-- =====================================================================================

-- ---------- A. 诊断（只读） ----------
-- A0. 依赖预检：应返回 3 行（缺哪张说明该区没建过对应备份，先停下贴回）
SELECT table_name FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('eq_card_bak_v2', 'eq_characters_bak_v2', 'equip_id_migration');

-- A1. 悬空持有行数（= 前端"丢失"装备量级）
SELECT COUNT(*) AS dangling_cnt
FROM eq_characters cs
WHERE cs.is_delete = '0'
  AND NOT EXISTS (SELECT 1 FROM eq_card e WHERE e.id = cs.id);

-- A2. eq_card 缺失的配置行数（备份有、现表 uuid 没有，如截图里 160 那种被折叠删掉的）
SELECT COUNT(*) AS cfg_missing
FROM eq_card_bak_v2 b
WHERE NOT EXISTS (SELECT 1 FROM eq_card e WHERE e.uuid = b.uuid);

-- A3. 悬空持有行按可修复路径预览（S4 才需要人工）：
--   S1 uuid 链可达（其配置行现仍在 eq_card，只是持有 id 没跟上）
--   S2 映射链可达（配置行被折叠，挂到组内保留行的 new_id）
--   S3 去尾号 base 映射可达（老式尾号孤儿）
--   S4 都到不了
SELECT
  CASE
    WHEN ec1.uuid IS NOT NULL THEN 'S1_uuid_chain'
    WHEN m.new_id IS NOT NULL AND EXISTS (SELECT 1 FROM eq_card e3 WHERE e3.id = m.new_id) THEN 'S2_map_chain'
    WHEN m2.new_id IS NOT NULL AND EXISTS (SELECT 1 FROM eq_card e4 WHERE e4.id = m2.new_id) THEN 'S3_base_chain'
    ELSE 'S4_manual'
  END AS kind,
  COUNT(*) AS cnt,
  GROUP_CONCAT(DISTINCT cs.id ORDER BY cs.id SEPARATOR ',') AS sample_ids
FROM eq_characters cs
LEFT JOIN eq_characters_bak_v2 hb ON hb.uuid = cs.uuid
LEFT JOIN eq_card_bak_v2 cb       ON cb.id   = hb.id
LEFT JOIN eq_card ec1             ON ec1.uuid = cb.uuid
LEFT JOIN equip_id_migration m    ON m.old_id = hb.id
LEFT JOIN equip_id_migration m2   ON m2.old_id = REGEXP_REPLACE(hb.id, '[0-9]+$', '')
WHERE cs.is_delete = '0'
  AND NOT EXISTS (SELECT 1 FROM eq_card e WHERE e.id = cs.id)
GROUP BY kind;

-- A4. 持有表整行缺失数（备份有、现表 uuid 没有 —— 即你说的 eq_characters 大量缺少）
SELECT COUNT(*) AS eq_characters_missing
FROM eq_characters_bak_v2 b
WHERE NOT EXISTS (SELECT 1 FROM eq_characters cs WHERE cs.uuid = b.uuid);

-- A5. 被软删的行清单（备份 is_delete='0'、现表 '1'：若曾执行过补全脚本 3a 误软删，先确认这份清单）
SELECT cs.uuid, cs.user_id, cs.id, cs.lv
FROM eq_characters cs
JOIN eq_characters_bak_v2 b ON b.uuid = cs.uuid
WHERE b.is_delete = '0' AND cs.is_delete = '1';

-- ---------- B. 找回 ----------
-- B0. 修复前再各备份一份现表（本次操作自己的回滚点）
CREATE TABLE IF NOT EXISTS eq_characters_bak_fix AS SELECT * FROM eq_characters;
CREATE TABLE IF NOT EXISTS eq_card_bak_fix       AS SELECT * FROM eq_card;

-- B1.【补齐配置】把现表缺失的 uuid 配置行从备份补回，uuid 沿用备份原值不重新生成，id 调整为映射新 id；
--     新 id 已被保留行占用的（折叠归并）跳过，名称/大类/介绍用 v2 值，属性保留该变体自身数值，被动清空
INSERT INTO eq_card (uuid, name, weight, star, id, profession, introduce, eq_type, eq_type2,
                     wl_atk, hy_atk, ds_atk, fd_atk, wl_def, hy_def, ds_def, fd_def, zl_def, zl_atk,
                     passive_introduce_one, passive_introduce_two, passive_introduce_three)
SELECT b.uuid,
       IF(m.old_id IS NULL, b.name, m.new_name),
       b.weight, b.star,
       IF(m.old_id IS NULL, b.id, m.new_id),
       b.profession,
       IF(m.old_id IS NULL, b.introduce, m.new_introduce),
       IF(m.old_id IS NULL, b.eq_type, m.new_eq_type),
       IF(m.old_id IS NULL, b.eq_type2, m.new_eq_type2),
       b.wl_atk, b.hy_atk, b.ds_atk, b.fd_atk,
       b.wl_def, b.hy_def, b.ds_def, b.fd_def, b.zl_def, b.zl_atk,
       NULL, NULL, NULL
FROM eq_card_bak_v2 b
LEFT JOIN equip_id_migration m ON m.old_id = b.id
WHERE NOT EXISTS (SELECT 1 FROM eq_card e  WHERE e.uuid = b.uuid)
  AND NOT EXISTS (SELECT 1 FROM eq_card e2 WHERE e2.id = IF(m.old_id IS NULL, b.id, m.new_id));

-- B2.【补齐持有行】备份里有、现表整行缺失的，按 uuid 原样补回（b.* 首列即原 uuid，保持不变，镶嵌/前端缓存按 uuid 认实例不受影响；lv/exp/xilian/flyup/is_delete 等全部用备份值）
--     ⚠ 若备份之后玩家正常分解/丢弃过装备，也会被补回；如有此情况先按 update_time 过滤再执行
INSERT INTO eq_characters
SELECT b.* FROM eq_characters_bak_v2 b
WHERE NOT EXISTS (SELECT 1 FROM eq_characters cs WHERE cs.uuid = b.uuid);

-- B3.【uuid 链重挂】持有行当前 id 悬空时，沿 持有uuid->备份老id->备份配置uuid->现表配置当前id 重挂
--     （不管现表那行现在叫老 id 还是新 id，都挂到真实存在的那行；B2 刚补回的老 id 行也在这里被重挂）
UPDATE eq_characters cs
JOIN eq_characters_bak_v2 hb ON hb.uuid  = cs.uuid
JOIN eq_card_bak_v2 cb       ON cb.id    = hb.id
JOIN eq_card ec              ON ec.uuid  = cb.uuid
SET cs.id = ec.id
WHERE cs.id <> ec.id
  AND NOT EXISTS (SELECT 1 FROM eq_card x WHERE x.id = cs.id);

-- B4.【映射链重挂】配置行被折叠归并到保留行的，按 备份老id -> new_id 重挂（仅当 new_id 配置确实存在）
UPDATE eq_characters cs
JOIN eq_characters_bak_v2 hb ON hb.uuid = cs.uuid
JOIN equip_id_migration m    ON m.old_id = hb.id
SET cs.id = m.new_id
WHERE NOT EXISTS (SELECT 1 FROM eq_card x WHERE x.id = cs.id)
  AND EXISTS (SELECT 1 FROM eq_card y WHERE y.id = m.new_id);

-- B5.【尾号孤儿重挂】当前 id 去尾号后的 base 在映射表里（如 P018_FK505 -> P018_FK 的新 id）
UPDATE eq_characters cs
JOIN equip_id_migration m ON m.old_id = REGEXP_REPLACE(cs.id, '[0-9]+$', '')
SET cs.id = m.new_id
WHERE NOT EXISTS (SELECT 1 FROM eq_card x WHERE x.id = cs.id)
  AND EXISTS (SELECT 1 FROM eq_card y WHERE y.id = m.new_id);

-- B6.【尾号孤儿重挂·备份源】同 B5，去尾号来源换成备份老 id（当前 id 已被改乱的行）
UPDATE eq_characters cs
JOIN eq_characters_bak_v2 hb ON hb.uuid = cs.uuid
JOIN equip_id_migration m    ON m.old_id = REGEXP_REPLACE(hb.id, '[0-9]+$', '')
SET cs.id = m.new_id
WHERE NOT EXISTS (SELECT 1 FROM eq_card x WHERE x.id = cs.id)
  AND EXISTS (SELECT 1 FROM eq_card y WHERE y.id = m.new_id);

-- B7.（仅当 A5 清单确认均为误软删后执行）恢复被软删的持有行
-- UPDATE eq_characters cs
-- JOIN eq_characters_bak_v2 b ON b.uuid = cs.uuid
-- SET cs.is_delete = '0'
-- WHERE b.is_delete = '0' AND cs.is_delete = '1';

-- ---------- C. 验证 ----------
-- C1. 悬空持有行应为 0（“eq_card 与 eq_characters 不缺”的最终判定）：
SELECT COUNT(*) AS orphan_cnt
FROM eq_characters cs
LEFT JOIN eq_card ec ON ec.id = cs.id
WHERE cs.is_delete = '0' AND ec.id IS NULL;
-- C1b. 持有表整行缺失应为 0：
SELECT COUNT(*) AS still_rows_missing
FROM eq_characters_bak_v2 b
WHERE NOT EXISTS (SELECT 1 FROM eq_characters cs WHERE cs.uuid = b.uuid);
-- C2. 补回的配置行数（应与 A2 减去折叠跳过数吻合）：
SELECT COUNT(*) AS still_missing
FROM eq_card_bak_v2 b
WHERE NOT EXISTS (SELECT 1 FROM eq_card e WHERE e.uuid = b.uuid);
-- C3. 抽样看重挂后的装备与配置：
SELECT cs.uuid, cs.user_id, cs.id, ec.name, ec.star, cs.lv
FROM eq_characters cs
JOIN eq_characters_bak_v2 hb ON hb.uuid = cs.uuid
JOIN eq_card ec ON ec.id = cs.id
WHERE hb.id <> cs.id
ORDER BY RAND() LIMIT 20;

-- ---------- D. 人工残留清单（S4：uuid 链、映射链、base 链都到不了的） ----------
SELECT cs.uuid, cs.user_id, cs.id, cs.lv, hb.id AS bak_id
FROM eq_characters cs
LEFT JOIN eq_characters_bak_v2 hb ON hb.uuid = cs.uuid
WHERE cs.is_delete = '0'
  AND NOT EXISTS (SELECT 1 FROM eq_card e WHERE e.id = cs.id)
ORDER BY cs.id;

-- B8.（2区适用，按需手动启用）D 段残留里若 bak_id 去尾号的 base 在备份配置里存在，
--     参照孤儿补全思路：从 base 配置行（经 B1/B3 后现表里的新 id 行）复制一行到孤儿当前 id 上，
--     使持有行直接可 JOIN。需人工逐 base 确认后再写具体 INSERT，模板：
-- INSERT INTO eq_card (name, weight, star, id, profession, introduce, eq_type, eq_type2,
--                      wl_atk, hy_atk, ds_atk, fd_atk, wl_def, hy_def, ds_def, fd_def, zl_def, zl_atk,
--                      passive_introduce_one, passive_introduce_two, passive_introduce_three)
-- SELECT src.name, src.weight, src.star, o.cs_id, src.profession, src.introduce,
--        src.eq_type, src.eq_type2,
--        src.wl_atk, src.hy_atk, src.ds_atk, src.fd_atk,
--        src.wl_def, src.hy_def, src.ds_def, src.fd_def, src.zl_def, src.zl_atk,
--        NULL, NULL, NULL
-- FROM (SELECT DISTINCT cs.id AS cs_id
--       FROM eq_characters cs
--       WHERE cs.is_delete='0' AND NOT EXISTS (SELECT 1 FROM eq_card e WHERE e.id=cs.id)) o
-- JOIN eq_card src ON src.id = REGEXP_REPLACE(o.cs_id, '[0-9]+$', '')
--   OR src.id = REGEXP_REPLACE(o.cs_id, '[0-9]+$', '_')  -- 按实际 base 形态调整匹配；src 为补回/改名后的配置行

-- ---------- G. 类别漂移修复（防具变宝具等） ----------
-- 成因：迁移在“已改过一轮 id”的中间态上又跑了一次 step2 映射，新老前缀撞车：
--   老Y腰带 -> 一轮变X靴(防具,正确) -> 二轮把 X 当“老宝具X”拆去 N项链/P披风(宝具)  ← 防具变宝具
--   老K法器 -> 一轮变Y印章(法器,正确) -> 二轮把 Y 当“老腰带Y”变 X靴(防具)        ← 法器变防具
-- 判定基准：以 eq_card_bak_v2（迁移前原始配置，uuid 对应）的老前缀按设计映射推出的“应得前缀”为准。
-- 注意：老盾D→新戒Z(宝具) 是迁移设计映射本身，不算漂移；若也不接受，另行告知目标子类再单独改。

-- G0. 构建漂移表（以原始老前缀为准；现编号段保留，星级段未变）
DROP TABLE IF EXISTS eq_drift_fix;
CREATE TABLE eq_drift_fix AS
SELECT ec.uuid,
       ec.id AS cur_id,
       cb.id AS orig_id,
       w.want_prefix,
       CONCAT(w.want_prefix, LPAD(w.num,3,'0'),
              IF(LOCATE('_', ec.id) > 0, SUBSTRING(ec.id, LOCATE('_', ec.id)),
                 CONCAT('_', LPAD(cb.uuid % 900 + 100, 3, '0')))       ) AS want_id,
       p.equip_name AS want_name,
       CONCAT(p.equip_name, '，取山川之精、铸百炼之形，', p.type_name, '中暗藏洪荒之力。') AS want_introduce,
       ELT(FIELD(w.want_prefix,'J','G','F','C','T','X','Z','N','P','Y','H','B'),0,0,0,1,1,1,2,2,2,3,3,3) AS want_eq_type,
       FIELD(w.want_prefix,'J','G','F','C','T','X','Z','N','P','Y','H','B') AS want_eq_type2
FROM eq_card ec
JOIN eq_card_bak_v2 cb ON cb.uuid = ec.uuid
JOIN (
  SELECT cb2.uuid,
    CASE LEFT(cb2.id,1)
      WHEN 'J' THEN 'J' WHEN 'G' THEN 'G' WHEN 'Q' THEN 'F'
      WHEN 'S' THEN 'C' WHEN 'M' THEN 'T' WHEN 'Y' THEN 'X'
      WHEN 'D' THEN 'Z' WHEN 'P' THEN 'P'
      WHEN 'X' THEN IF(CRC32(cb2.id) % 2 = 0, 'N', 'P')
      WHEN 'K' THEN ELT(1 + CRC32(cb2.id) % 3, 'Y', 'H', 'B')
      WHEN 'F' THEN 'F'
      ELSE LEFT(cb2.id,1) END AS want_prefix,
    CAST(SUBSTRING(SUBSTRING_INDEX(ec2.id, '_', 1), 2) AS UNSIGNED) AS num
  FROM eq_card ec2
  JOIN eq_card_bak_v2 cb2 ON cb2.uuid = ec2.uuid
) w ON w.uuid = ec.uuid
JOIN eq_name_pool p ON p.prefix = w.want_prefix AND p.num = w.num
WHERE LEFT(ec.id,1) <> w.want_prefix;

-- G1. 漂移总览（先看这个，贴回结果；应主要是 X→N/P、Y→X 两类）
SELECT want_prefix, LEFT(cur_id,1) AS cur_prefix,
       COUNT(*) AS cfg_cnt, GROUP_CONCAT(cur_id ORDER BY cur_id SEPARATOR ',') AS sample
FROM eq_drift_fix
GROUP BY want_prefix, LEFT(cur_id,1) ORDER BY cfg_cnt DESC;

-- G2. 受影响的玩家持有装备清单（当前挂着漂移配置的行）
SELECT cs.uuid, cs.user_id, cs.id AS cur_id, d.want_id, d.want_prefix
FROM eq_characters cs
JOIN eq_drift_fix d ON d.cur_id = cs.id
WHERE cs.is_delete = '0'
ORDER BY d.want_prefix, cs.id;

-- G3. 修复（确认 G1 后执行）
-- G3a. want_id 已被其它行占用（折叠冲突）：持有行先改挂到占用行，再删漂移行
UPDATE eq_characters cs
JOIN eq_drift_fix d ON cs.id = d.cur_id
JOIN eq_card e ON e.id = d.want_id AND e.uuid <> d.uuid
SET cs.id = d.want_id;

DELETE ec FROM eq_card ec
JOIN eq_drift_fix d ON d.uuid = ec.uuid
WHERE EXISTS (SELECT 1 FROM eq_card x WHERE x.id = d.want_id AND x.uuid <> d.uuid);

-- G3b. 无冲突的漂移行：改回应得 id/名称/介绍/大类子类
UPDATE eq_card ec
JOIN eq_drift_fix d ON d.uuid = ec.uuid
SET ec.id        = d.want_id,
    ec.name      = d.want_name,
    ec.introduce = d.want_introduce,
    ec.eq_type   = d.want_eq_type,
    ec.eq_type2  = d.want_eq_type2
WHERE LEFT(ec.id,1) <> d.want_prefix;

-- G3c. 持有行中仍指向漂移旧 id 的会变悬空 → 重跑一遍 B3、B4 链即可挂正（幂等）

-- ---------- H. 盾(D)→衣服(C)：按用户决定修正设计映射（防具归防具） ----------
-- 背景：迁移设计把老盾 D(防具) 归到了戒指 Z(宝具)。本段把“原始是盾、当前不是防具”的配置行
-- 改回衣服 C：id 前缀 Z→C（编号段/后缀不动，同星级段对 C 同样合法），名称/介绍取自 C 名池，
-- eq_type=1(防具)、eq_type2=4(衣服)。若目标 id 已被其它行占用，则折叠：持有改挂占用行、删本行。
-- 幂等：重跑时已改好的行不再命中条件。

-- H0. 构建盾修正表
DROP TABLE IF EXISTS eq_shield_fix;
CREATE TABLE eq_shield_fix AS
SELECT ec.uuid,
       ec.id AS cur_id,
       CONCAT('C', SUBSTRING(ec.id, 2)) AS want_id,
       p.equip_name AS want_name,
       CONCAT(p.equip_name, '，取山川之精、铸百炼之形，', p.type_name, '中暗藏洪荒之力。') AS want_introduce
FROM eq_card ec
JOIN eq_card_bak_v2 cb ON cb.uuid = ec.uuid
JOIN eq_name_pool p ON p.prefix = 'C'
   AND p.num = CAST(SUBSTRING(SUBSTRING_INDEX(ec.id, '_', 1), 2) AS UNSIGNED)
WHERE LEFT(cb.id, 1) = 'D'      -- 原始配置是盾
  AND ec.eq_type <> 1;          -- 当前却不是防具（被归成宝具戒指等）

-- H1. 预览：将改回衣服的盾配置行与受影响持有装备数（贴回确认后再跑 H2~H4）
SELECT s.cur_id, s.want_id, s.want_name, cb.name AS orig_name,
       (SELECT COUNT(*) FROM eq_characters cs WHERE cs.is_delete='0' AND cs.id = s.cur_id) AS hold_cnt
FROM eq_shield_fix s
JOIN eq_card_bak_v2 cb ON cb.uuid = s.uuid
ORDER BY s.cur_id;

-- H2. 折叠冲突：want_id 已被其它行占用的，持有先改挂占用行，再删盾行
UPDATE eq_characters cs
JOIN eq_shield_fix s ON cs.id = s.cur_id
JOIN eq_card e ON e.id = s.want_id AND e.uuid <> s.uuid
SET cs.id = s.want_id;

DELETE ec FROM eq_card ec
JOIN eq_shield_fix s ON s.uuid = ec.uuid
WHERE EXISTS (SELECT 1 FROM eq_card x WHERE x.id = s.want_id AND x.uuid <> s.uuid);

-- H3. 无冲突的盾行：改回衣服 id/名称/介绍/大类子类
UPDATE eq_card ec
JOIN eq_shield_fix s ON s.uuid = ec.uuid
SET ec.id        = s.want_id,
    ec.name      = s.want_name,
    ec.introduce = s.want_introduce,
    ec.eq_type   = 1,
    ec.eq_type2  = 4
WHERE ec.id = s.cur_id;

-- H4. 持有行同步改挂到改回后的衣服 id（指向被改名盾行的装备）
UPDATE eq_characters cs
JOIN eq_shield_fix s ON cs.id = s.cur_id
SET cs.id = s.want_id;

-- H5. 验证：不应再有“原始是盾、当前不是防具”的持有装备（应为 0）
SELECT COUNT(*) AS shield_still_treasure
FROM eq_characters cs
JOIN eq_card ec ON ec.id = cs.id
JOIN eq_characters_bak_v2 hb ON hb.uuid = cs.uuid
JOIN eq_card_bak_v2 cb ON cb.id = hb.id
WHERE cs.is_delete = '0' AND LEFT(cb.id,1) = 'D' AND ec.eq_type <> 1;
-- 再跑一遍 C1 确认悬空仍为 0；装备业务实时查库无需重启，玩家重登即可看到新子类

-- ---------- E. 善后（确认无误后另行执行） ----------
-- DROP TABLE eq_characters_bak_fix; DROP TABLE eq_card_bak_fix;
