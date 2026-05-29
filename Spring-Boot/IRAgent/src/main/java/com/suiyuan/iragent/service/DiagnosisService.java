package com.suiyuan.iragent.service;

import com.suiyuan.iragent.dag.core.DagGraph;
import com.suiyuan.iragent.dag.core.NodeResult;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface DiagnosisService {

    Map<String, NodeResult> diagnose(String question, String studentAnswer,
                                      String subjectType, String userId,
                                      BiConsumer<String, String> onTextChunk,
                                      Consumer<NodeResult> onNodeComplete);

    DagGraph getDiagnosisDag();
}
