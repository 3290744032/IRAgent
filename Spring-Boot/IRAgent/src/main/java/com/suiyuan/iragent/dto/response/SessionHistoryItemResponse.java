package com.suiyuan.iragent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionHistoryItemResponse {
    private String sessionId;
    private String question;
    private String topic;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private Integer totalSteps;
    private Integer masteredSteps;
}
