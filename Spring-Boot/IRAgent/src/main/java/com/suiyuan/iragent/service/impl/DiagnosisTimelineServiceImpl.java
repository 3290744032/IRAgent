package com.suiyuan.iragent.service.impl;

import com.suiyuan.iragent.dag.core.NodeResult;
import com.suiyuan.iragent.service.DiagnosisTimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class DiagnosisTimelineServiceImpl implements DiagnosisTimelineService {

    @Override
    public List<Map<String, Object>> convertToTimeline(Map<String, NodeResult> results) {
        List<Map<String, Object>> timeline = new ArrayList<>();

        timeline.add(createAction("write_text", Map.of("content", "【错题诊断报告】")));
        timeline.add(createAction("audio_trigger", Map.of("time", 0)));

        NodeResult prerequisite = results.get("prerequisite_check");
        if (prerequisite != null && prerequisite.isSuccess()) {
            timeline.add(createAction("highlight", Map.of("section", "前置考点漏缺")));
            timeline.add(createAction("write_text", Map.of("content", extractContent(prerequisite))));
            timeline.add(createAction("audio_trigger", Map.of("time", 3)));
        }

        NodeResult formula = results.get("formula_confusion");
        if (formula != null && formula.isSuccess()) {
            timeline.add(createAction("highlight", Map.of("section", "核心公式混淆")));
            timeline.add(createAction("write_text", Map.of("content", extractContent(formula))));
            timeline.add(createAction("write_formula", Map.of("content", extractFormulaContent(formula))));
            timeline.add(createAction("audio_trigger", Map.of("time", 6)));
        }

        NodeResult calculation = results.get("calculation_error");
        if (calculation != null && calculation.isSuccess()) {
            timeline.add(createAction("highlight", Map.of("section", "计算步骤失误")));
            timeline.add(createAction("write_text", Map.of("content", extractContent(calculation))));
            timeline.add(createAction("audio_trigger", Map.of("time", 9)));
        }

        NodeResult aggregate = results.get("aggregate");
        if (aggregate != null && aggregate.isSuccess()) {
            timeline.add(createAction("highlight", Map.of("section", "综合诊断")));
            timeline.add(createAction("write_text", Map.of("content", extractSummaryContent(aggregate))));
            timeline.add(createAction("audio_trigger", Map.of("time", 12)));
        }

        timeline.add(createAction("write_text", Map.of("content", "诊断完成。请针对以上薄弱点进行针对性练习。")));

        return timeline;
    }

    private Map<String, Object> createAction(String type, Map<String, Object> params) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", type);
        action.put("timestamp", System.currentTimeMillis());
        action.putAll(params);
        return action;
    }

    @SuppressWarnings("unchecked")
    private String extractContent(NodeResult result) {
        Map<String, Object> output = result.getOutput();
        if (output == null) return "无数据";
        Object analysis = output.get("analysis");
        if (analysis != null) return analysis.toString();
        Object content = output.get("content");
        return content != null ? content.toString() : output.toString();
    }

    @SuppressWarnings("unchecked")
    private String extractFormulaContent(NodeResult result) {
        Map<String, Object> output = result.getOutput();
        if (output == null) return "";
        Object confusions = output.get("confusions");
        if (confusions instanceof List<?> list && !list.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    sb.append("❌ ").append(m.get("wrong")).append(" → ✅ ").append(m.get("correct")).append("\n");
                }
            }
            return sb.toString();
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private String extractSummaryContent(NodeResult result) {
        Map<String, Object> output = result.getOutput();
        if (output == null) return "无数据";
        Object summary = output.get("summary");
        return summary != null ? summary.toString() : output.toString();
    }
}
