package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

@Data
@TableName("app_version")
public class AppVersion {
    @TableId(value = "appid", type = IdType.AUTO)
    private Integer appid;
    @TableField("version")
    private String version;
    @TableField("description")
    private String description;
    @TableField("wgt_url")
    private String wgtUrl;
    @TableField("pkg_url")
    private String pkgUrl;
    @TableField("is_hbuilder_update")
    private Integer isHbuilderUpdate;
    @TableField("is_force_update")
    private Integer isForceUpdate;


}