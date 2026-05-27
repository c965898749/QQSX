package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

import java.util.Date;
@Data
@TableName("daily_view_content")
public class DailyViewContent {
    @TableId(value = "content_id", type = IdType.AUTO)
    private Long contentId;
    @TableField("gift_id")
    private Long giftId;
    @TableField("item_type")
    private Integer itemType;
    @TableField("item_id")
    private Long itemId;
    @TableField("item_quantity")
    private Integer itemQuantity;
    @TableField("create_time")
    private Date createTime;
    @TableField("item_name")
    private String itemName;
    @TableField("icon")
    private String icon;

}