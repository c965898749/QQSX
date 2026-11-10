-- =====================================================================
-- 功勋商城价格配置表 game_gongxun_shop
-- 背景：原先 getGongxunStore / buyGongxunStore 的价格硬编码为 quality*100，
--       现改为由本配置表统一管理（商品成员、价格、排序、上下架都由表驱动）。
-- 主库(192.168.0.106)：商品名/图标/描述仍来自 game_item_base，本表只配“卖不卖、卖多少、怎么排”。
-- =====================================================================

CREATE TABLE IF NOT EXISTS game_gongxun_shop (
    item_id     INT         NOT NULL COMMENT '商品 item_id，对应 game_item_base.item_id（功勋商城段 item_id>=1100000）',
    price       INT         NOT NULL DEFAULT 0 COMMENT '功勋售价（消耗 user.gongxun）',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '展示排序，升序；同值按 item_id 升序',
    is_valid    TINYINT     NOT NULL DEFAULT 1 COMMENT '是否上架：1上架 0下架',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (item_id),
    KEY idx_valid_sort (is_valid, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='功勋商城价格配置表';

-- ---------------------------------------------------------------------
-- 初始化数据（迁移现有硬编码行为）：把 game_item_base 中 item_id>=1100000 的
-- 全部商品按原 quality*100 价格写入配置表，保证上线前后价格一致、无回归。
-- 之后运营可直接 UPDATE game_gongxun_shop.price / is_valid / sort 精调，不再影响卡表。
-- ---------------------------------------------------------------------
INSERT INTO game_gongxun_shop (item_id, price, sort, is_valid)
SELECT b.item_id, IFNULL(b.quality, 0) * 100, b.item_id, 1
FROM game_item_base b
WHERE b.item_id >= 1100000
ON DUPLICATE KEY UPDATE price = VALUES(price), sort = VALUES(sort);

-- ---------------------------------------------------------------------
-- 示例：定向英雄卡类商品（itemId/1000 = 英雄卡id，见 useItem 的 redian 映射）。
-- 若上面的迁移 SELECT 已覆盖这些行，这里用 ON DUPLICATE 只做“手工定价覆盖”，
-- 可按需修改价格 / 增删行。示范三种不同价位的配置写法。
-- ---------------------------------------------------------------------
INSERT INTO game_gongxun_shop (item_id, price, sort, is_valid) VALUES
    (1105000, 5000, 1105000, 1),  -- 定向英雄卡 1105
    (1106000, 5000, 1106000, 1),  -- 定向英雄卡 1106
    (1107000, 8000, 1107000, 1)   -- 定向英雄卡 1107（更稀有，价高）
ON DUPLICATE KEY UPDATE price = VALUES(price), sort = VALUES(sort);
