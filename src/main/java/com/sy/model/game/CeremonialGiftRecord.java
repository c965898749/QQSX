package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

import java.util.Date;
@Data
@TableName("ceremonial_gift_record")
public class CeremonialGiftRecord {
    @TableId(value = "record_id", type = IdType.AUTO)
    private Long recordId;
    @TableField("item_id")
    private Integer itemId;
    @TableField("item_type")
    private Integer itemType;
    @TableField("weight")
    private Integer weight;
    @TableField("award")
    private Integer award;
    @TableField("txt")
    private String txt;
    @TableField("icon")
    private String icon;
    @TableField("user_id")
    private Integer userId;
    @TableField("get_time")
    private Date getTime;


}