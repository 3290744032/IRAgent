package com.suiyuan.iragent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearningStepResponse {
    private Integer stepId;
    private Integer index;
    private String question;
    private String hint;
    private String knowledgePoint;
    private String userAnswer;
    private String aiFeedback;
    private Integer attempts;
    private String status;
    private LocalDateTime answeredAt;
}
