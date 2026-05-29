package com.suiyuan.iragent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("learning_steps")
public class LearningStep {
    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("session_id")
    private String sessionId;

    @TableField("step_index")
    private Integer stepIndex;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("status")
    private String status;

    @TableField("mastered_at")
    private LocalDateTime masteredAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
