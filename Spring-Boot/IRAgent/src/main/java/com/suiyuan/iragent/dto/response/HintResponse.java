package com.suiyuan.iragent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HintResponse {
    private String hint;
    private Integer hintsUsed;
    private Integer maxHints;
}
