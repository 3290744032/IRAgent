package com.suiyuan.iragent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("learning_sessions")
public class LearningSession {
    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("session_id")
    private String sessionId;

    @TableField("user_id")
    private Long userId;

    @TableField("question")
    private String question;

    @TableField("topic")
    private String topic;

    @TableField("subject_type")
    private String subjectType;

    @TableField("total_steps")
    private Integer totalSteps;

    @TableField("current_step")
    private Integer currentStep;

    @TableField("status")
    private String status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;
}
