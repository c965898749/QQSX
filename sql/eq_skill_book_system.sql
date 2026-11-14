-- =====================================================================================
-- 装备技能书系统（Phase 1：数据结构 + 配置，不接入战斗结算）
-- 执行库：master（游戏主库 yimem）
--
-- 流程口径（与前端 EqLianHuaCtrl / eqJiNengShuSelectCardCtrl 对齐）：
--   · 点「确认」= 消耗 1 本技能书，从对应池按权重随机抽 1 个技能（仅预览，不落库）。
--   · 点「baocun(保存)」= 消耗凝神石，把预览技能写入该装备的已学列表(Content)。
--   · 已学列表里每条技能的「baocun(替换)」= 免费把它设为「使用中」(is_active=1)。
--   · 相同技能不能重复保存；不能把与当前使用中相同的技能再替换。
--   · 凝神石消耗 cost = 档位基础值(低10/中20/高40) + step(5) × 该装备当前已学技能数。
--   · 技能等级不落库，实时按装备宝石计算：level = max(1, min(50, floor(Σ宝石等级 / ceil(星级))))（=宝石平均等级）。
--   · 装备穿戴判定沿用 go_into_num != 0；穿戴英雄 hero_id = characters.id(同站位)。
--   · 本期 effect_type / effect_value 仅预留，不改变战斗数值。
--   · ⚠ 数值平衡：等级能到 50（宝石满级 Lv50），所以本文件里所有 effect_value 已按 ÷5 折算，
--     最终数值 = 折算后每级值 × 等级，满级恰好等于最初的“每级值 ×10”设计区间。
--     改等级公式与改 effect_value 只能动一边，否则数值会翻倍叠加（详见 GameServiceServiceImpl#calcEqSkillLevel）。
--
-- ⚠ 执行前务必备库。
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- 1. 道具沿用 game_item_base 中已存在的条目（本系统不再新建，只做映射与背包计数）
--    凝神石   item_id = 557
--    初级技能书 item_id = 454
--    中级技能书 item_id = 455
--    高级技能书 item_id = 456
--    技能书不走 handleBagItemUse 的“使用”，而是在炼化面板经 eqDrawSkill 消耗。
--    （若上述道具尚未建好，请运营在 game_item_base 里按这些 id 补齐，本脚本不覆盖既有配置）
-- -------------------------------------------------------------------------------------

-- -------------------------------------------------------------------------------------
-- 2. eq_skill_base（装备技能表：技能静态定义 + 抽取池归属 + 权重）
--    tier：0低 1中 2高（通用池档位）；scope：0通用池 1英雄池
--    hero_id：scope=1 时非空，对应 characters.id（穿戴英雄卡编码）；scope=0 时为 NULL
--    weight：抽取权重（同池内相对概率）；effect_type 预留（本期不生效）；
--    effect_value = 每级效果值（非最终值），实际数值 = effect_value × 技能等级(=宝石平均等级，1~50)；
--    下面的数值已按 ÷5 折算过（原设计值/5），目的是让 Lv50 落在原设计的 Lv10 数值上；
--    description 中的 {v} 为数值占位符，后端按等级回填成具体数字后再下发。
-- -------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS eq_skill_base (
    skill_id     INT           NOT NULL AUTO_INCREMENT COMMENT '技能ID',
    name         VARCHAR(64)   NOT NULL COMMENT '技能名',
    tier         TINYINT       NOT NULL DEFAULT 0 COMMENT '档位：0低 1中 2高',
    hero_id      VARCHAR(32)   DEFAULT NULL COMMENT '英雄专属技能对应英雄卡id(characters.id)；通用技能为NULL',
    scope        TINYINT       NOT NULL DEFAULT 0 COMMENT '池：0通用池 1英雄池',
    description  VARCHAR(255)  DEFAULT NULL COMMENT '技能描述',
    weight       INT           NOT NULL DEFAULT 1 COMMENT '抽取权重(同池内相对概率)',
    effect_type  INT           DEFAULT NULL COMMENT '预留：战斗效果类型',
    effect_value DECIMAL(12,2) DEFAULT NULL COMMENT '每级效果值(非最终值)；实际数值=effect_value×技能等级；NULL=无次数值(如净化类)',
    create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (skill_id),
    KEY idx_pool (scope, tier, hero_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='装备技能表(技能池)';

-- 通用池数值修正（低/中/高三档；effect_value=每级数值【已 ÷5 折算】）
--   · 只用 UPDATE 改 effect_value，不再用 INSERT ... ON DUPLICATE KEY UPDATE：
--     后者会把运营在库里改过的 name / description / weight 一并冲回种子值。
--   · 写的是绝对值，重复执行结果一致（幂等）。
--   · ⚠ 本段不再负责建数据：若是全新库（表是空的），UPDATE 影响 0 行，
--     需先手工插入下面这 12 行，或从 git 历史版本取原来的 INSERT 段执行。
--   · ⚠ 本段也不管 effect_type：当年建表把它当预留列、INSERT 里没写，所以这 16 条（含下面 13~16）
--     的 effect_type 在库里是 NULL。补标签的 UPDATE 在 eq_skill_base_100.sql 末尾（幂等，可直接重跑）；
--     目前无任何代码读取 effect_type，NULL 不影响线上，只影响以后实装战斗结算时的按类归类。
UPDATE eq_skill_base SET effect_value = 0.40 WHERE skill_id = 1;   -- 破甲     无视防御%   → 满级 20%
UPDATE eq_skill_base SET effect_value = 0.60 WHERE skill_id = 2;   -- 嗜血     吸血%       → 满级 30%
UPDATE eq_skill_base SET effect_value = 0.40 WHERE skill_id = 3;   -- 铁骨     物理减伤%   → 满级 20%
UPDATE eq_skill_base SET effect_value = 1.00 WHERE skill_id = 4;   -- 迅捷     速度点      → 满级 50 点
UPDATE eq_skill_base SET effect_value = 0.80 WHERE skill_id = 5;   -- 连击     追击几率%   → 满级 40%
UPDATE eq_skill_base SET effect_value = 0.60 WHERE skill_id = 6;   -- 格挡     抵挡几率%   → 满级 30%
UPDATE eq_skill_base SET effect_value = 0.80 WHERE skill_id = 7;   -- 狂暴     攻击上限%   → 满级 40%
UPDATE eq_skill_base SET effect_value = 1.20 WHERE skill_id = 8;   -- 法术精通 技能系数%   → 满级 60%
UPDATE eq_skill_base SET effect_value = 1.00 WHERE skill_id = 9;   -- 不灭     防御%       → 满级 50%
UPDATE eq_skill_base SET effect_value = 0.60 WHERE skill_id = 10;  -- 天罡护体 反弹%       → 满级 30%
UPDATE eq_skill_base SET effect_value = 1.60 WHERE skill_id = 11;  -- 致命一击 暴击几率%   → 满级 80%
UPDATE eq_skill_base SET effect_value = 2.00 WHERE skill_id = 12;  -- 万象归一 净化几率%   → 满级 100%（嫌强可改 1.60 = 80%）

-- 检查：应为 12；小于 12 说明有行不存在，对应的 UPDATE 没生效，需先补建行
SELECT COUNT(*) AS 通用池行数, SUM(effect_value IS NULL) AS 未配数值的行
FROM eq_skill_base WHERE scope = 0 AND skill_id BETWEEN 1 AND 12;

-- 英雄池种子（scope=1，示例；运营按需在 characters.id 对应英雄上扩展行）
INSERT INTO eq_skill_base (skill_id, name, tier, hero_id, scope, description, weight, effect_value) VALUES
    (13, '烈焰焚天',   2, '101', 1, '该英雄专属：攻击附加灼烧，每层灼烧造成{v}%最大生命持续伤害。', 50,  0.20),
    (14, '寒冰护盾',   2, '101', 1, '该英雄专属：受击时生成吸收{v}%最大生命的护盾。',               50,  1.00),
    (15, '疾风连斩',   2, '102', 1, '该英雄专属：出手极快，攻速提升{v}%。',                       100, 1.00),
    (16, '大地的怒吼', 2, '103', 1, '该英雄专属：普攻附带{v}%几率范围眩晕。',                     100, 2.00)
ON DUPLICATE KEY UPDATE name=VALUES(name), tier=VALUES(tier), hero_id=VALUES(hero_id), scope=VALUES(scope), description=VALUES(description), weight=VALUES(weight), effect_value=VALUES(effect_value);

-- -------------------------------------------------------------------------------------
-- 3. eq_skill_book（技能书 -> 抽取池映射 + 槽位容量）
--    book_item_id：关联 game_item_base.item_id
--    draw_scope：0 从通用池按 tier 抽；1 从英雄池按穿戴英雄 hero_id 抽
--    hero_id：一般留 NULL（运行时取穿戴英雄）；若某本书固定绑定英雄则填 characters.id
--    max_slot：该装备可保存的技能总数上限（含使用中 1 条）
-- -------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS eq_skill_book (
    book_item_id INT     NOT NULL COMMENT '技能书道具id，对应 game_item_base.item_id',
    tier         TINYINT NOT NULL DEFAULT 0 COMMENT '通用池档位：0低 1中 2高(draw_scope=0生效)；draw_scope=1可忽略',
    draw_scope   TINYINT NOT NULL DEFAULT 0 COMMENT '抽取池：0通用 1英雄',
    hero_id      VARCHAR(32) DEFAULT NULL COMMENT '固定绑定英雄卡id；NULL表示运行时取该装备穿戴英雄',
    max_slot     INT     NOT NULL DEFAULT 5 COMMENT '该装备可保存技能总数上限(含使用中)',
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (book_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能书配置(池映射+槽位)';

INSERT INTO eq_skill_book (book_item_id, tier, draw_scope, hero_id, max_slot) VALUES
    (454, 0, 0, NULL, 5),  -- 初级技能书 -> 通用低级池
    (455, 1, 0, NULL, 5),  -- 中级技能书 -> 通用中级池
    (456, 2, 0, NULL, 5)   -- 高级技能书 -> 通用高级池
ON DUPLICATE KEY UPDATE tier=VALUES(tier), draw_scope=VALUES(draw_scope), hero_id=VALUES(hero_id), max_slot=VALUES(max_slot);
-- 英雄特定技能书：本期未提供 book_item_id，待运营在 game_item_base 建好后按 (book_item_id, tier, 1, NULL, 5) 追加一行即可（draw_scope=1 走穿戴英雄的英雄池）。

-- -------------------------------------------------------------------------------------
-- 4. eq_character_skill（装备已学技能：1 使用中 + 其余待使用）
--    eq_uuid：关联 eq_characters.uuid（一件具体装备）
--    is_active：1使用中 / 0待使用，同一 eq_uuid 至多 1 条 =1
--    slot_index：列表展示序号（0 起）
--    注：技能等级不落库，实时按装备宝石计算
-- -------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS eq_character_skill (
    uuid        INT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    eq_uuid     INT      NOT NULL COMMENT '装备实例uuid，对应 eq_characters.uuid',
    user_id     INT      NOT NULL COMMENT '所属用户，便于归属校验',
    skill_id    INT      NOT NULL COMMENT '技能id，关联 eq_skill_base.skill_id',
    slot_index  INT      NOT NULL DEFAULT 0 COMMENT '展示序号(0起)',
    is_active   TINYINT  NOT NULL DEFAULT 0 COMMENT '1使用中 0待使用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '获得时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (uuid),
    KEY idx_eq (eq_uuid),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='装备已学技能(使用中+待使用)';
