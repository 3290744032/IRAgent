package com.suiyuan.iragent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suiyuan.iragent.config.VolcEngineStreamingClient;
import com.suiyuan.iragent.dag.core.*;
import com.suiyuan.iragent.dag.engine.DagExecutor;
import com.suiyuan.iragent.dag.engine.DagExecutor.NodeHandler;
import com.suiyuan.iragent.dag.nodes.AggregateNode;
import com.suiyuan.iragent.dag.nodes.LlmCallNode;
import com.suiyuan.iragent.dag.nodes.TransformNode;
import com.suiyuan.iragent.service.DiagnosisService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
@Service
public class DiagnosisServiceImpl implements DiagnosisService {

    private final VolcEngineStreamingClient deepSeekStreamingClient;
    private final ObjectMapper objectMapper;
    private DagExecutor executor;
    private DagGraph diagnosisDag;

    public DiagnosisServiceImpl(@Qualifier("deepSeekStreamingClient") VolcEngineStreamingClient deepSeekStreamingClient, ObjectMapper objectMapper) {
        this.deepSeekStreamingClient = deepSeekStreamingClient;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() throws IOException {
        ClassPathResource resource = new ClassPathResource("dag/diagnosis-dag.json");
        diagnosisDag = objectMapper.readValue(resource.getInputStream(), DagGraph.class);

        Map<String, NodeHandler> handlers = Map.of(
                "LLM_CALL", new LlmCallNode(deepSeekStreamingClient),
                "TRANSFORM", new TransformNode(),
                "AGGREGATE", new AggregateNode()
        );

        executor = new DagExecutor(handlers);
        log.info("DAG 诊断引擎初始化完成: name={}, nodes={}", diagnosisDag.getName(), diagnosisDag.getNodes().size());
    }

    @Override
    public Map<String, NodeResult> diagnose(String question, String studentAnswer,
                                              String subjectType, String userId,
                                              BiConsumer<String, String> onTextChunk,
                                              Consumer<NodeResult> onNodeComplete) {
        ExecutionContext context = new ExecutionContext(userId);
        context.putVariable("question", question);
        context.putVariable("studentAnswer", studentAnswer != null ? studentAnswer : "");
        context.putVariable("subjectType", subjectType);
        context.putVariable("_onTextChunk", onTextChunk);

        log.info("开始错题诊断: userId={}, subjectType={}", userId, subjectType);
        Map<String, NodeResult> results = executor.execute(diagnosisDag, context, onNodeComplete);
        log.info("错题诊断完成: userId={}, 节点数={}", userId, results.size());
        return results;
    }

    @Override
    public DagGraph getDiagnosisDag() {
        return diagnosisDag;
    }
}
