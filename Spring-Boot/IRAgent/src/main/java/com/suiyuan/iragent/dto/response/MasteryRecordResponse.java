package com.suiyuan.iragent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MasteryRecordResponse {
    private String knowledgePoint;
    private String topic;
    private Integer proficiency;
    private String status;
    private Integer reviewCount;
    private LocalDateTime lastReviewedAt;
    private List<String> misconceptions;
}
