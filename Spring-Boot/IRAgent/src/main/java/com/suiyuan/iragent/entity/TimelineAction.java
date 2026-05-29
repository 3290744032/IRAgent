package com.suiyuan.iragent.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineAction {

    private String id;

    private double time;

    private String action;

    private String text;

    private String tts;

    private String latex;

    private String expr;

    private String target;

    @Builder.Default
    private double duration = 0;

    private Map<String, Object> params;

    private Map<String, Object> config;

    @Builder.Default
    private Boolean audioTrigger = false;

    private Double audioDuration;
}
