package com.suiyuan.iragent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mastery_records")
public class MasteryRecord {
    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("user_id")
    private Long userId;

    @TableField("knowledge_point")
    private String knowledgePoint;

    @TableField("topic")
    private String topic;

    @TableField("proficiency")
    private Integer proficiency;

    @TableField("review_count")
    private Integer reviewCount;

    @TableField("last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    @TableField("next_review_at")
    private LocalDateTime nextReviewAt;

    @TableField("misconceptions")
    private String[] misconceptions;

    @TableField("status")
    private String status;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
