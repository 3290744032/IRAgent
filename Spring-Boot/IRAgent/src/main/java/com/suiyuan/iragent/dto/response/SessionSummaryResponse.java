package com.suiyuan.iragent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionSummaryResponse {
    private String sessionId;
    private String topic;
    private String question;
    private String totalTime;
    private LocalDateTime completedAt;
    private KnowledgeGraphResponse knowledgeGraph;
    private MasterySummaryResponse masterySummary;
    private List<HistoryItemResponse> history;
    private List<RecommendationResponse> recommendations;
}
