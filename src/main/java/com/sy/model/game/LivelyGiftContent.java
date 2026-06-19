package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;
@Data
@TableName("lively_gift_content")
public class LivelyGiftContent {
    @TableId(value = "content_id", type = IdType.AUTO)
    private Long contentId;
    @TableField("gift_id")
    private Long giftId;
    @TableField("item_type")
    private Byte itemType;
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