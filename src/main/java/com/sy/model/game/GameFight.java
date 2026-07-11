package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
@TableName("game_fight")
public class GameFight {
    @TableId(value = "id", type = IdType.AUTO)
    private String id;
    @TableField("user_id")
    private Integer userId;
    @TableField("to_user_id")
    private Integer toUserId;
    @TableField("fightter")
    private String fightter;
    @TableField("user_name")
    private String userName;
    @TableField("to_user_name")
    private String toUserName;
    @TableField("type")
    private String  type;//0pve、1竞技场2、好有pk
    @TableField("createtime")
    private Date createtime;
    @TableField("is_win")
    private Integer isWin;//0赢1输
    @TableField("img")
    private String img;
    @TableField(exist = false)
    private String timeStr;
}