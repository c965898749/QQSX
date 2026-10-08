package com.sy.model.game;

import lombok.Data;

@Data
public class GameItemShop {
    private Integer itemId;

    private String itemName;

    private String quality;

    private Integer goldEdgePrice;

    private Integer gemPrice;

    private Integer stock;

    private String itemDesc;

    private String type;

    private String icon;

    private Integer id;

    private Integer isBuy;

    // 折扣档位：0=原价，3/5/8 表示 3折/5折/8折（限时商城生成时随机分配）
    private Integer discount;

    // 应付价格：按 discount 折算后的价格，前端展示与购买结算都用它
    private Integer payPrice;
}