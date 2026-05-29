package com.suiyuan.iragent.enums;

import lombok.Getter;

@Getter
public enum SessionStatus {
    IN_PROGRESS("in_progress", "学习中"),
    COMPLETED("completed", "已完成"),
    ABANDONED("abandoned", "已放弃");

    private final String value;
    private final String description;

    SessionStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
