package com.suiyuan.iragent.dto.sse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackSSEEvent {
    private String type;
    private Object data;
}
