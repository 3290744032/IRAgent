package com.suiyuan.iragent_app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * P3 协议：原子动作模型
 * 对应后端 com.suiyuan.iragent.video.timeline.TimelineAction
 */
public class TimelineAction {

    @SerializedName("id")
    private String id;

    @SerializedName("time")
    private double time;

    @SerializedName("action")
    private String action;

    @SerializedName("text")
    private String text;

    @SerializedName("latex")
    private String latex;

    @SerializedName("expr")
    private String expr;

    @SerializedName("target")
    private String target;

    @SerializedName("duration")
    private double duration;

    @SerializedName("audioTrigger")
    private Boolean audioTrigger;

    @SerializedName("audioDuration")
    private Double audioDuration;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getTime() { return time; }
    public void setTime(double time) { this.time = time; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public ActionType getActionType() { return ActionType.fromString(action); }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getLatex() { return latex; }
    public void setLatex(String latex) { this.latex = latex; }

    public String getExpr() { return expr; }
    public void setExpr(String expr) { this.expr = expr; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public double getDuration() { return duration; }
    public void setDuration(double duration) { this.duration = duration; }

    public Boolean getAudioTrigger() { return audioTrigger; }
    public void setAudioTrigger(Boolean audioTrigger) { this.audioTrigger = audioTrigger; }
    public boolean shouldTriggerAudio() { return audioTrigger != null && audioTrigger; }

    public Double getAudioDuration() { return audioDuration; }
    public void setAudioDuration(Double audioDuration) { this.audioDuration = audioDuration; }
}
