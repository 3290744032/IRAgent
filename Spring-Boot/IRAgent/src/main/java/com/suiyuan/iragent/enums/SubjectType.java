package com.suiyuan.iragent.enums;

import lombok.Getter;

@Getter
public enum SubjectType {
    GENERAL("general", "通用综合"),
    PROGRAMMING("programming", "编程"),
    MATH("math", "数学"),
    PHYSICS("physics", "物理"),
    CHEMISTRY("chemistry", "化学"),
    BIOLOGY("biology", "生物"),
    ENGLISH("english", "英语"),
    HISTORY("history", "历史");

    private final String value;
    private final String description;

    SubjectType(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
