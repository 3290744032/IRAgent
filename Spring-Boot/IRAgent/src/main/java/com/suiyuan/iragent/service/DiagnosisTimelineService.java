package com.suiyuan.iragent.service;

import com.suiyuan.iragent.dag.core.NodeResult;

import java.util.List;
import java.util.Map;

public interface DiagnosisTimelineService {

    List<Map<String, Object>> convertToTimeline(Map<String, NodeResult> results);
}
