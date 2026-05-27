package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

import java.util.Date;
@Data
@TableName("friend_blessing")
public class FriendBlessing {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("sender_id")
    private Integer senderId;
    @TableField("receiver_id")
    private Integer receiverId;
    @TableField("content")
    private String content;
    @TableField("send_time")
    private Date sendTime;
    @TableField("is_read")
    private Integer isRead;

}