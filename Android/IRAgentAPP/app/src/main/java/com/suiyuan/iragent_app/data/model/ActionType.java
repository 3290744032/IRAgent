package com.suiyuan.iragent_app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * P3 协议：原子动作类型枚举
 * 对应后端 com.suiyuan.iragent.video.timeline.TimelineAction.action
 */
public enum ActionType {

    @SerializedName("title") TITLE,
    @SerializedName("write_text") WRITE_TEXT,
    @SerializedName("write_formula") WRITE_FORMULA,
    @SerializedName("draw_graph") DRAW_GRAPH,
    @SerializedName("show_grid") SHOW_GRID,
    @SerializedName("highlight") HIGHLIGHT;

    public static ActionType fromString(String value) {
        if (value == null) return null;
        for (ActionType t : values()) {
            if (t.name().equalsIgnoreCase(value)) return t;
        }
        return null;
    }
}
