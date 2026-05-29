package com.suiyuan.iragent.dto.sse;

import com.suiyuan.iragent.dto.response.ProgressResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackCompleteData {
    private String fullFeedback;
    private String evaluation;
    private Boolean isCorrect;
    private ProgressResponse progress;
    private Boolean isCompleted;
    private String summaryUrl;
}
