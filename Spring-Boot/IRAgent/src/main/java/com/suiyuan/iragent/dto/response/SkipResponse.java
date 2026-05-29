package com.suiyuan.iragent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkipResponse {
    private SkippedStepResponse skippedStep;
    private Boolean isCompleted;
    private ProgressResponse progress;
}
