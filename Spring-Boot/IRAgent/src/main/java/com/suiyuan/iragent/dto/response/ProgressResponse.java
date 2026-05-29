package com.suiyuan.iragent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgressResponse {
    private Integer currentStep;
    private Integer totalSteps;
    private Integer masteredSteps;
    private Integer remainingSteps;
    private Integer skippedSteps;
}
