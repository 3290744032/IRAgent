package com.suiyuan.iragent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.suiyuan.iragent.handler.StringArrayTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("learning_summaries")
public class LearningSummary {
    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("session_id")
    private String sessionId;

    @TableField("topic")
    private String topic;

    @TableField("question")
    private String question;

    @TableField("total_time")
    private String totalTime;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("knowledge_graph")
    private String knowledgeGraph;

    @TableField("mastery_summary")
    private String masterySummary;

    @TableField(value = "misconceptions", typeHandler = StringArrayTypeHandler.class)
    private String[] misconceptions;

    @TableField(value = "recommendations", typeHandler = StringArrayTypeHandler.class)
    private String[] recommendations;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
