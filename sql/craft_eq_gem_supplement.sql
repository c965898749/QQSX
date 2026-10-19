-- =====================================================================
-- craft_eq 宝石配置补齐脚本（开发库 192.168.0.106:3307/yimem）
-- 目的：
--   A. 补齐六类宝石各自 lv50 行（现有 type=0/1/2 每段只有 lv1~49）
--   B. 补齐法器(type='3')的宝石配置（此前完全缺失，导致镶嵌返回"暂未开放"）
--
-- 现有分段规律（已核实）：
--   type='0' 兵刃 → 太阳石(39~88, 物理伤害) + 红玛瑙(250~299, 法术伤害)   —— 进攻系
--   type='1' 防具 → 月亮石(200~249, 法术抗性) + 光芒石(350~399, 物理抗性) —— 防御系
--   type='2' 宝具 → 黑玉石(300~349, 治疗效果) + 神秘石(400~449, 被治疗效果)—— 治疗系
--   type='3' 法器 → 本次开放：太阳石(39~88) + 红玛瑙(250~299)，与兵刃同为进攻系装备
--
-- 等级映射规律（已核实）：level = item_id - 段起始ID + 1
--   太阳石: lv = id - 38    红玛瑙: lv = id - 249
-- icon 直接取自 game_item_base 中同名宝石物品的 icon（craft_eq.icon 与之一致）
-- 执行前请先备份：CREATE TABLE craft_eq_bak_20261020 AS SELECT * FROM craft_eq;
-- =====================================================================

-- ---------- A. 补齐 lv50 行（每类宝石末级：88/249/299/349/399/449） ----------
INSERT INTO craft_eq (item_id, `type`, icon, `level`)
SELECT x.item_id, x.`type`, i.icon, 50
FROM (
              SELECT '0' AS `type`,   88 AS item_id   -- 太阳石 Lv50（兵刃）
    UNION ALL SELECT '0',             299              -- 红玛瑙 Lv50（兵刃）
    UNION ALL SELECT '1',             249              -- 月亮石 Lv50（防具）
    UNION ALL SELECT '1',             399              -- 光芒石 Lv50（防具）
    UNION ALL SELECT '2',             349              -- 黑玉石 Lv50（宝具）
    UNION ALL SELECT '2',             449              -- 神秘石 Lv50（宝具）
) x
JOIN game_item_base i ON i.item_id = x.item_id AND i.item_type = 6
WHERE NOT EXISTS (
    SELECT 1 FROM craft_eq c WHERE c.item_id = x.item_id AND c.`type` = x.`type`
);

-- ---------- B. 法器 type='3'：太阳石 39~88（Lv1~50） ----------
INSERT INTO craft_eq (item_id, `type`, icon, `level`)
SELECT i.item_id, '3', i.icon, i.item_id - 38
FROM game_item_base i
WHERE i.item_type = 6
  AND i.item_id BETWEEN 39 AND 88
  AND NOT EXISTS (
      SELECT 1 FROM craft_eq c WHERE c.item_id = i.item_id AND c.`type` = '3'
  );

-- ---------- B. 法器 type='3'：红玛瑙 250~299（Lv1~50） ----------
INSERT INTO craft_eq (item_id, `type`, icon, `level`)
SELECT i.item_id, '3', i.icon, i.item_id - 249
FROM game_item_base i
WHERE i.item_type = 6
  AND i.item_id BETWEEN 250 AND 299
  AND NOT EXISTS (
      SELECT 1 FROM craft_eq c WHERE c.item_id = i.item_id AND c.`type` = '3'
  );

-- ---------- 验证 ----------
-- 预期：type=0/1/2/3 各 100 行，共 400 行；每段 lv 覆盖 1~50
SELECT `type`,
       CASE WHEN item_id < 200 THEN '太阳石'
            WHEN item_id < 250 THEN '月亮石'
            WHEN item_id < 300 THEN '红玛瑙'
            WHEN item_id < 350 THEN '黑玉石'
            WHEN item_id < 400 THEN '光芒石'
            ELSE '神秘石' END AS gem,
       COUNT(*) AS cnt, MIN(`level`) AS min_lv, MAX(`level`) AS max_lv
FROM craft_eq
GROUP BY `type`, gem
ORDER BY CAST(`type` AS UNSIGNED), item_id;
