package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 功勋商城价格配置：商品成员/价格/排序/上下架由本表统一管理，
 * 商品名/图标/描述仍取自 game_item_base（按 item_id 关联）。
 */
@Data
@TableName("game_gongxun_shop")
public class GameGongxunShop {
    @TableId(value = "item_id", type = IdType.INPUT)
    private Integer itemId;
    @TableField("price")
    private Integer price;
    @TableField("sort")
    private Integer sort;
    @TableField("is_valid")
    private Byte isValid;
    @TableField("create_time")
    private Date createTime;
    @TableField("update_time")
    private Date updateTime;
}
