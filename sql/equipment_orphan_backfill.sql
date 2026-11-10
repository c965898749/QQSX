-- =====================================================================================
-- 装备孤儿数据补全脚本
-- 背景：eq_characters 里存在某些 id（如 P018_FK505），在 eq_card 中找不到对应配置行，
--       导致登录时 LEFT JOIN 出 null 属性（formateEqCharacter 已做兜底置0，但装备显示为空属性）。
--       尾号(505)是旧 eq_card 行的自增 uuid，本身无业务含义；
--       同一"基础配置"(前缀+编号+属性字母相同、仅 uuid 不同)在 eq_card 中通常还有兄弟行。
-- 策略：按"去尾号后的 base_id"匹配一个兄弟配置行，复制一行到孤儿 id 上（保留玩家装备、不重随属性）。
--       匹配不到兄弟行的（该基础配置在 eq_card 已彻底不存在），列出来人工处理（删除或重建）。
-- ⚠ 执行前务必备库；需 MySQL 8.0+（用到 REGEXP_REPLACE）。执行后重启后端刷新 EQ_CARD_CACHE。
-- =====================================================================================

-- ---------- 0. 备份 ----------
CREATE TABLE IF NOT EXISTS eq_card_bak_orphan AS SELECT * FROM eq_card;

-- ---------- 1. 补建前先看看孤儿分布（去重后的基础配置有多少、能否找到兄弟行） ----------
-- SELECT
--   REGEXP_REPLACE(cs.id,'[0-9]+$','') AS base_id,
--   COUNT(*) AS orphan_rows,
--   (SELECT COUNT(*) FROM eq_card e WHERE REGEXP_REPLACE(e.id,'[0-9]+$','')=REGEXP_REPLACE(cs.id,'[0-9]+$','')) AS sibling_cnt
-- FROM eq_characters cs
-- WHERE cs.is_delete='0' AND NOT EXISTS (SELECT 1 FROM eq_card e WHERE e.id=cs.id)
-- GROUP BY base_id ORDER BY sibling_cnt DESC;

-- ---------- 2. 从兄弟配置行复制补建（仅补 base_id 能在 eq_card 找到同基础配置的行） ----------
INSERT INTO eq_card
  (name, weight, star, id, profession, introduce, eq_type, eq_type2,
   wl_atk, hy_atk, ds_atk, fd_atk, wl_def, hy_def, ds_def, fd_def, zl_def, zl_atk,
   passive_introduce_one, passive_introduce_two, passive_introduce_three)
SELECT
  src.name, src.weight, src.star, o.orphan_id, src.profession, src.introduce,
  src.eq_type, src.eq_type2,
  src.wl_atk, src.hy_atk, src.ds_atk, src.fd_atk,
  src.wl_def, src.hy_def, src.ds_def, src.fd_def, src.zl_def, src.zl_atk,
  NULL, NULL, NULL
FROM (
  -- 每个孤儿 id 及其去尾号后的 base_id（去重）
  SELECT DISTINCT cs.id AS orphan_id, REGEXP_REPLACE(cs.id, '[0-9]+$', '') AS base_id
  FROM eq_characters cs
  WHERE cs.is_delete = '0'
    AND NOT EXISTS (SELECT 1 FROM eq_card e WHERE e.id = cs.id)
) o
JOIN (
  -- eq_card 中每个 base_id 取一个代表行（MIN(id)）
  SELECT REGEXP_REPLACE(id, '[0-9]+$', '') AS base_id, MIN(id) AS sample_id
  FROM eq_card
  GROUP BY REGEXP_REPLACE(id, '[0-9]+$', '')
) g ON g.base_id = o.base_id
JOIN eq_card src ON src.id = g.sample_id;

-- ---------- 3. 补建后仍无法关联的（该基础配置在 eq_card 已彻底不存在，无兄弟可复制） ----------
-- 这些只能二选一：① 直接软删除玩家装备；② 参照 equipment_v2_migration.sql 的名称池/星级规则重建。
SELECT cs.uuid, cs.user_id, cs.id, cs.lv
FROM eq_characters cs
WHERE cs.is_delete = '0'
  AND NOT EXISTS (SELECT 1 FROM eq_card e WHERE e.id = cs.id)
ORDER BY cs.id;

-- ---------- 3a.（可选）把上面残留的孤儿装备软删除，避免继续显示空属性 ----------
-- UPDATE eq_characters cs
-- SET cs.is_delete = '1'
-- WHERE cs.is_delete = '0'
--   AND NOT EXISTS (SELECT 1 FROM eq_card e WHERE e.id = cs.id);

-- ---------- 4. 验证：持有孤儿应为 0 ----------
SELECT COUNT(*) AS orphan_cnt
FROM eq_characters cs
LEFT JOIN eq_card ec ON ec.id = cs.id
WHERE cs.is_delete = '0' AND ec.id IS NULL;

-- ---------- 5. 善后（确认无误后另行执行） ----------
-- DROP TABLE eq_card_bak_orphan;
