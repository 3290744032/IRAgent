package com.suiyuan.iragent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MasterResponse {
    private String status;
    private NextStepInfo nextStep;
    private Boolean isCompleted;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NextStepInfo {
        private Integer index;
        private String title;
    }
}
