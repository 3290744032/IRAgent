package com.suiyuan.iragent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSessionResponse {
    private String sessionId;
    private String topic;
    private Integer totalSteps;
    private Integer currentStep;
    private String status;
    private LocalDateTime createdAt;
    private List<StepInfo> steps;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepInfo {
        private Integer index;
        private String title;
        private String content;
    }
}
