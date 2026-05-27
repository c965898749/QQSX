package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

import java.util.Date;
@Data
@TableName("daily_view_finsh")
public class DailyViewFinsh {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("gift_code")
    private String giftCode;
    @TableField("user_id")
    private Integer userId;
    @TableField("get_time")
    private Date getTime;
}