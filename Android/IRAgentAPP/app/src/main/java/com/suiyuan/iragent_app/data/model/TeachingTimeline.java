package com.suiyuan.iragent_app.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * P3 协议：教学时间轴总览
 * 对应后端 com.suiyuan.iragent.video.timeline.TeachingTimeline
 *
 * renderer.html 的 playTimeline(json) 接收此结构，
 * JS 端根据 durationSeconds 和时间轴动作序列驱动 Canvas 动画。
 */
public class TeachingTimeline {

    @SerializedName("lessonTitle")
    private String lessonTitle;

    @SerializedName("topic")
    private String topic;

    @SerializedName("durationSeconds")
    private int durationSeconds = 60;

    @SerializedName("style")
    private String style;

    @SerializedName("viewBox")
    private ViewBox viewBox;

    @SerializedName("timeline")
    private List<TimelineAction> timeline;

    public String getLessonTitle() { return lessonTitle; }
    public void setLessonTitle(String lessonTitle) { this.lessonTitle = lessonTitle; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }

    public ViewBox getViewBox() { return viewBox; }
    public void setViewBox(ViewBox viewBox) { this.viewBox = viewBox; }

    public List<TimelineAction> getTimeline() { return timeline; }
    public void setTimeline(List<TimelineAction> timeline) { this.timeline = timeline; }
}
