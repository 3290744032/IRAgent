package com.suiyuan.iragent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolveResponse {
    
    private String solution;
    
    private List<ResponseSegment> segments;
    
    private Long userId;
    
    private String conversationId;
}