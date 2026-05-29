package com.suiyuan.iragent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 会话实体类
 * 用于存储用户与AI的聊天会话信息
 */
@Data
@TableName("conversation")
public class Conversation {
    /**
     * 会话ID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String conversationId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会话名称
     */
    private String name;

    /**
     * 会话描述
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 会话状态：active（活跃）、inactive（非活跃）
     */
    private String status;
}
