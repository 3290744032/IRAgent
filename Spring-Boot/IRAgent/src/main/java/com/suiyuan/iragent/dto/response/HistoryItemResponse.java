package com.suiyuan.iragent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoryItemResponse {
    private Integer stepId;
    private String question;
    private String userAnswer;
    private String feedback;
    private String evaluation;
    private LocalDateTime timestamp;
}
