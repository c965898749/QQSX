package com.sy.model.game;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

@Data
public class ChatMsg implements Serializable {
    private static final long serialVersionUID = 1L;

    // 频道类型 1世界 2洞府 3私聊
    private Integer channelType;
    // 消息文本/资源链接
    private String content;
    // 发送人名称
    private String senderName;
    // 内容类型 1文本 2视频 3语音
    private Integer contentType;
    // 发送人ID
    private Long senderId;
    // 目标ID：私聊=对方ID / 洞府=洞府ID / 世界=0
    private Long targetId;
    // 消息唯一ID
    private Long msgId;

    private String itemId;
    // 发送时间
    private String sendTime;
}
