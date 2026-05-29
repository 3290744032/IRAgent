package com.suiyuan.iragent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MasterySummaryResponse {
    private String overall;
    private List<KnowledgeMasteryResponse> knowledgePoints;
    private Integer totalSteps;
    private Integer mastered;
    private Integer needsReview;
}
