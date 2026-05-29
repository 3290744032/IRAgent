package com.suiyuan.iragent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkippedStepResponse {
    private Integer stepId;
    private Integer index;
    private String question;
    private String explanation;
    private String example;
}
