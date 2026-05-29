package com.suiyuan.iragent.enums;

import lombok.Getter;

@Getter
public enum EvaluationResult {
    CORRECT("correct", "完全正确"),
    PARTIAL("partial", "部分正确"),
    INCORRECT("incorrect", "理解有误");

    private final String value;
    private final String description;

    EvaluationResult(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
