package com.suiyuan.iragent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionDetailResponse {
    private String sessionId;
    private Long userId;
    private String question;
    private String topic;
    private String subjectType;
    private Integer totalSteps;
    private Integer currentStep;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private List<LearningStepResponse> steps;
}
