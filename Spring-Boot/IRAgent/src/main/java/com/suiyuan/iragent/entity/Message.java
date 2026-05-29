package com.suiyuan.iragent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 消息实体类
 * 用于存储会话中的具体消息
 */
@Data
@TableName("message")
public class Message {
    /**
     * 消息ID
     */
    @TableId(type = IdType.AUTO)
    private Long messageId;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 发送者类型：user（用户）、ai（AI）
     */
    private String senderType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型：text（文本）、image（图片）、file（文件）
     */
    private String messageType;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 消息状态：sent（已发送）、failed（发送失败）
     */
    private String status;
}
