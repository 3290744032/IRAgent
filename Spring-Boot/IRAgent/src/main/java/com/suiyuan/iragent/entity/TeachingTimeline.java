package com.suiyuan.iragent.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeachingTimeline {

    private String lessonTitle;

    private String topic;

    @Builder.Default
    private int durationSeconds = 60;

    @Builder.Default
    private String style = "blackboard";

    @Builder.Default
    private ViewBox viewBox = ViewBox.builder().build();

    private List<TimelineAction> timeline;
}
