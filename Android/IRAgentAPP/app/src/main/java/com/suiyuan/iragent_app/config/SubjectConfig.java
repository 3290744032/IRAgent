package com.suiyuan.iragent_app.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 学科配置 — 全项目统一，新增学科只需改这一个文件
 */
public final class SubjectConfig {

    private SubjectConfig() {}

    /** 全部支持学科（不含"全部"选项，用于后端分类） */
    public static final List<String> ALL_SUBJECTS = Collections.unmodifiableList(
            Arrays.asList("数学", "物理", "化学", "英语", "政治", "历史", "生物", "地理"));

    /** 带"全部"选项，用于前端筛选标签 */
    public static final List<String> SUBJECTS_WITH_ALL = Collections.unmodifiableList(
            Arrays.asList("全部", "数学", "物理", "化学", "英语", "政治", "历史", "生物", "地理"));

    /** 默认学科（拍照批改等场景） */
    public static final String DEFAULT_SUBJECT = "数学";
}
