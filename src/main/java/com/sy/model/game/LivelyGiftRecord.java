package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;
@Data
@TableName("lively_gift_record")
public class LivelyGiftRecord {
    @TableId(value = "record_id", type = IdType.AUTO)
    private Long recordId;
    @TableField("user_id")
    private Long userId;
    @TableField("gift_id")
    private Long giftId;
    @TableField("gift_code")
    private String giftCode;
    @TableField("get_time")
    private Date getTime;
    @TableField("status")
    private Integer status;
    @TableField("fail_reason")
    private String failReason;
    @TableField("platform")
    private String platform;
    @TableField("ip_address")
    private String ipAddress;

   }