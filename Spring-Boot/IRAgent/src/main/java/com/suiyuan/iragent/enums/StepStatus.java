package com.suiyuan.iragent.enums;

import lombok.Getter;

@Getter
public enum StepStatus {
    PENDING("pending", "待学习"),
    LEARNING("learning", "学习中"),
    MASTERED("mastered", "已掌握"),
    SKIPPED("skipped", "已跳过"),
    RETRY("retry", "需重试");

    private final String value;
    private final String description;

    StepStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
