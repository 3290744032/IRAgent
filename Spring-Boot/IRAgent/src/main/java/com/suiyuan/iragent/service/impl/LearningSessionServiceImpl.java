package com.suiyuan.iragent.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suiyuan.iragent.config.PromptTemplateManager;
import com.suiyuan.iragent.dto.response.CreateSessionResponse;
import com.suiyuan.iragent.dto.response.MasterResponse;
import com.suiyuan.iragent.entity.LearningSession;
import com.suiyuan.iragent.entity.LearningStep;
import com.suiyuan.iragent.entity.LearningSummary;
import com.suiyuan.iragent.entity.MasteryRecord;
import com.suiyuan.iragent.enums.SessionStatus;
import com.suiyuan.iragent.enums.StepStatus;
import com.suiyuan.iragent.mapper.LearningSessionMapper;
import com.suiyuan.iragent.mapper.LearningStepMapper;
import com.suiyuan.iragent.mapper.LearningSummaryMapper;
import com.suiyuan.iragent.mapper.MasteryRecordMapper;
import com.suiyuan.iragent.service.AIProxyService;
import com.suiyuan.iragent.service.LearningSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningSessionServiceImpl extends ServiceImpl<LearningSessionMapper, LearningSession>
        implements LearningSessionService {

    private final LearningStepMapper learningStepMapper;
    private final LearningSummaryMapper learningSummaryMapper;
    private final MasteryRecordMapper masteryRecordMapper;
    private final AIProxyService aiProxyService;
    private final ObjectMapper objectMapper;
    private final PromptTemplateManager promptTemplateManager;
    private final AsyncTaskExecutor asyncTaskExecutor;

    private final ConcurrentHashMap<String, List<Map<String, String>>> conversationHistories = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> roundCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<String>> completedModules = new ConcurrentHashMap<>();

    private static final String[] MODULE_NAMES = {
        "核心知识点铺垫",
        "题目分析与思路",
        "分步详细讲解",
        "总结与归纳"
    };

    private static final String[] MODULE_DESCRIPTIONS = {
        "讲解本题需要用到的核心概念、公式和基础知识",
        "引导学生分析题目的已知条件、未知条件和解题方向",
        "一步步推导和计算，展示完整的解题过程",
        "回顾整题的关键点，形成知识框架"
    };

    @Override
    @Transactional
    public CreateSessionResponse createSession(Long userId, String question, String subjectType) {
        String sessionId = IdUtil.fastSimpleUUID();

        LearningSession session = new LearningSession();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setQuestion(question);
        session.setSubjectType(subjectType);
        session.setStatus(SessionStatus.IN_PROGRESS.getValue());
        session.setTotalSteps(0);
        session.setCurrentStep(0);
        session.setCreatedAt(LocalDateTime.now());

        baseMapper.insert(session);
        log.info("创建学习会话: sessionId={}, userId={}, question={}", sessionId, userId, question);

        CompletableFuture.runAsync(() -> {
            try {
                log.info("异步生成知识点: sessionId={}", sessionId);
                generateTeachingStepsAsync(sessionId, question, subjectType);
                log.info("异步生成知识点完成: sessionId={}", sessionId);
            } catch (Exception e) {
                log.error("异步生成知识点失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            }
        }, asyncTaskExecutor);

        List<CreateSessionResponse.StepInfo> defaultSteps = createDefaultSteps(sessionId, question);

        return new CreateSessionResponse(
                sessionId,
                "正在生成学习内容...",
                defaultSteps.size(),
                0,
                SessionStatus.IN_PROGRESS.getValue(),
                session.getCreatedAt(),
                defaultSteps
        );
    }

    private void generateTeachingStepsAsync(String sessionId, String question, String subjectType) {
        try {
            Map<String, Object> teachingResult = aiProxyService.generateTeachingContent(question, subjectType);

            LearningSession session = getSessionById(sessionId);
            if (session == null) {
                log.warn("会话不存在: sessionId={}", sessionId);
                return;
            }

            List<CreateSessionResponse.StepInfo> steps = new ArrayList<>();
            String topic = "学习主题";

            if (teachingResult.containsKey("_error")) {
                log.warn("AI生成知识点失败: sessionId={}", sessionId);
            } else {
                topic = (String) teachingResult.getOrDefault("topic", "学习主题");

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> stepList = (List<Map<String, Object>>) teachingResult.get("steps");

                if (stepList != null && !stepList.isEmpty()) {
                    learningStepMapper.delete(new QueryWrapper<LearningStep>().eq("session_id", sessionId));

                    int index = 1;
                    for (Map<String, Object> stepData : stepList) {
                        LearningStep step = new LearningStep();
                        step.setSessionId(sessionId);
                        step.setStepIndex(index);
                        step.setTitle((String) stepData.getOrDefault("title", "知识点 " + index));
                        step.setContent((String) stepData.getOrDefault("content", ""));
                        step.setStatus(StepStatus.PENDING.getValue());
                        step.setCreatedAt(LocalDateTime.now());
                        step.setUpdatedAt(LocalDateTime.now());
                        learningStepMapper.insert(step);

                        steps.add(new CreateSessionResponse.StepInfo(
                                index,
                                step.getTitle(),
                                step.getContent()
                        ));
                        index++;
                    }
                }
            }

            if (!steps.isEmpty()) {
                session.setTopic(topic);
                session.setTotalSteps(steps.size());
                baseMapper.updateById(session);
            }

        } catch (Exception e) {
            log.error("异步生成知识点异常: sessionId={}, error={}", sessionId, e.getMessage(), e);
        }
    }

    private List<CreateSessionResponse.StepInfo> createDefaultSteps(String sessionId, String question) {
        List<CreateSessionResponse.StepInfo> steps = new ArrayList<>();
        
        String[] defaultTitles = {"问题理解", "基本概念", "核心原理", "实践应用", "总结回顾"};
        
        for (int i = 0; i < defaultTitles.length; i++) {
            int index = i + 1;
            LearningStep step = new LearningStep();
            step.setSessionId(sessionId);
            step.setStepIndex(index);
            step.setTitle(defaultTitles[i]);
            step.setContent("## " + defaultTitles[i] + "\n\n这部分内容将帮助您理解：" + question);
            step.setStatus(StepStatus.PENDING.getValue());
            step.setCreatedAt(LocalDateTime.now());
            step.setUpdatedAt(LocalDateTime.now());
            learningStepMapper.insert(step);
            
            steps.add(new CreateSessionResponse.StepInfo(index, step.getTitle(), step.getContent()));
        }
        
        return steps;
    }

    @Override
    public Map<String, Object> getSessionDetail(String sessionId) {
        LearningSession session = getSessionById(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }
        
        QueryWrapper<LearningStep> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId);
        wrapper.orderByAsc("step_index");
        List<LearningStep> steps = learningStepMapper.selectList(wrapper);
        
        Map<String, Object> result = BeanUtil.beanToMap(session, "id");
        result.put("steps", steps.stream().map(this::convertStepToMap).collect(Collectors.toList()));
        
        return result;
    }

    private Map<String, Object> convertStepToMap(LearningStep step) {
        Map<String, Object> map = new HashMap<>();
        map.put("index", step.getStepIndex());
        map.put("title", step.getTitle());
        map.put("content", step.getContent());
        map.put("status", step.getStatus());
        map.put("masteredAt", step.getMasteredAt());
        return map;
    }

    @Override
    public List<LearningSession> getSessionsByUserId(Long userId) {
        QueryWrapper<LearningSession> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.orderByDesc("created_at");
        return baseMapper.selectList(wrapper);
    }

    @Override
    public Map<String, Object> getSessionHistory(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        
        QueryWrapper<LearningSession> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.orderByDesc("created_at");
        wrapper.last("LIMIT " + size + " OFFSET " + offset);
        List<LearningSession> sessions = baseMapper.selectList(wrapper);
        
        QueryWrapper<LearningSession> countWrapper = new QueryWrapper<>();
        countWrapper.eq("user_id", userId);
        long total = baseMapper.selectCount(countWrapper);
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("sessions", sessions);
        
        return result;
    }

    @Override
    @Transactional
    public boolean deleteSession(String sessionId, Long userId) {
        LearningSession session = getSessionById(sessionId);
        if (session == null) {
            return false;
        }
        
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除该会话");
        }
        
        QueryWrapper<LearningStep> stepWrapper = new QueryWrapper<>();
        stepWrapper.eq("session_id", sessionId);
        learningStepMapper.delete(stepWrapper);
        
        QueryWrapper<LearningSummary> summaryWrapper = new QueryWrapper<>();
        summaryWrapper.eq("session_id", sessionId);
        learningSummaryMapper.delete(summaryWrapper);
        
        baseMapper.deleteById(session.getId());
        
        log.info("删除学习会话: sessionId={}, userId={}", sessionId, userId);
        return true;
    }

    @Override
    public Map<String, Object> getSessionSummary(String sessionId) {
        LearningSession session = getSessionById(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }
        
        QueryWrapper<LearningSummary> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId);
        LearningSummary summary = learningSummaryMapper.selectOne(wrapper);
        
        if (summary == null) {
            return generateSummary(session);
        }
        
        return convertSummaryToResponse(summary);
    }

    @Override
    public boolean hasSummaryCache(String sessionId) {
        QueryWrapper<LearningSummary> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId);
        return learningSummaryMapper.selectCount(wrapper) > 0;
    }

    @Override
    @Transactional
    public Map<String, Object> saveGeneratedSummary(String sessionId, String aiResponseJson) {
        LearningSession session = getSessionById(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }

        String totalTime = calculateTotalTime(session.getCreatedAt(), session.getCompletedAt());

        try {
            // 清理Markdown格式，提取纯JSON
            String cleanedJson = cleanJsonResponse(aiResponseJson);
            Map<String, Object> summaryResult = objectMapper.readValue(cleanedJson, Map.class);

            LearningSummary summary = new LearningSummary();
            summary.setSessionId(sessionId);
            summary.setTopic(session.getTopic());
            summary.setQuestion(session.getQuestion());
            summary.setTotalTime(totalTime);
            summary.setCompletedAt(session.getCompletedAt());

            Object kgObj = summaryResult.get("knowledgeGraph");
            summary.setKnowledgeGraph(kgObj != null ? objectMapper.writeValueAsString(kgObj) : null);

            Object msObj = summaryResult.get("masterySummary");
            summary.setMasterySummary(msObj != null ? objectMapper.writeValueAsString(msObj) : null);

            Object recObj = summaryResult.get("recommendations");
            if (recObj instanceof List) {
                summary.setRecommendations(((List<?>) recObj).toArray(new String[0]));
            }

            summary.setCreatedAt(LocalDateTime.now());
            learningSummaryMapper.insert(summary);

            return convertSummaryToResponse(summary);
        } catch (Exception e) {
            log.error("保存流式总结数据失败: sessionId={}", sessionId, e);
            throw new RuntimeException("保存总结数据失败: " + e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> generateSummary(LearningSession session) {
        QueryWrapper<LearningStep> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", session.getSessionId());
        wrapper.eq("status", StepStatus.MASTERED.getValue());
        long masteredCount = learningStepMapper.selectCount(wrapper);
        
        String totalTime = calculateTotalTime(session.getCreatedAt(), session.getCompletedAt());
        
        Map<String, Object> summaryResult = aiProxyService.generateSummary(
                session.getQuestion(),
                (int) masteredCount,
                totalTime
        );
        
        LearningSummary summary = new LearningSummary();
        summary.setSessionId(session.getSessionId());
        summary.setTopic(session.getTopic());
        summary.setQuestion(session.getQuestion());
        summary.setTotalTime(totalTime);
        summary.setCompletedAt(session.getCompletedAt());
        
        try {
            if (!summaryResult.containsKey("_error")) {
                Object kgObj = summaryResult.get("knowledgeGraph");
                summary.setKnowledgeGraph(kgObj != null ? objectMapper.writeValueAsString(kgObj) : null);
                
                Object msObj = summaryResult.get("masterySummary");
                summary.setMasterySummary(msObj != null ? objectMapper.writeValueAsString(msObj) : null);
                
                Object recObj = summaryResult.get("recommendations");
                if (recObj instanceof List) {
                    summary.setRecommendations(((List<?>) recObj).toArray(new String[0]));
                }
            }
        } catch (Exception e) {
            log.error("学习总结数据处理失败", e);
        }
        
        summary.setCreatedAt(LocalDateTime.now());
        learningSummaryMapper.insert(summary);
        
        return convertSummaryToResponse(summary);
    }

    private String calculateTotalTime(LocalDateTime start, LocalDateTime end) {
        if (start == null) {
            return "未知";
        }
        LocalDateTime endTime = end != null ? end : LocalDateTime.now();
        Duration duration = Duration.between(start, endTime);
        long minutes = duration.toMinutes();
        if (minutes < 60) {
            return minutes + "分钟";
        }
        long hours = minutes / 60;
        minutes = minutes % 60;
        return hours + "小时" + minutes + "分钟";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertSummaryToResponse(LearningSummary summary) {
        Map<String, Object> result = BeanUtil.beanToMap(summary,
                "id", "knowledgeGraph", "masterySummary", "misconceptions", "recommendations", "createdAt");
        
        try {
            if (summary.getKnowledgeGraph() != null) {
                result.put("knowledgeGraph", objectMapper.readValue(summary.getKnowledgeGraph(), Map.class));
            }
            if (summary.getMasterySummary() != null) {
                result.put("masterySummary", objectMapper.readValue(summary.getMasterySummary(), Map.class));
            }
        } catch (Exception e) {
            log.error("总结数据转换失败", e);
        }
        
        if (summary.getRecommendations() != null) {
            List<Map<String, String>> recList = new ArrayList<>();
            for (String rec : summary.getRecommendations()) {
                Map<String, String> recMap = new HashMap<>();
                recMap.put("description", rec);
                int colonIdx = rec.indexOf("：");
                if (colonIdx > 0 && colonIdx < 20) {
                    recMap.put("title", rec.substring(0, colonIdx).trim());
                } else {
                    recMap.put("title", rec.length() > 15 ? rec.substring(0, 15) + "..." : rec);
                }
                recList.add(recMap);
            }
            result.put("recommendations", recList);
        } else {
            result.put("recommendations", new ArrayList<>());
        }
        
        return result;
    }

    @Override
    @Transactional
    public MasterResponse markAsMastered(String sessionId, Integer stepIndex) {
        LearningSession session = getSessionById(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }
        
        LearningStep step = getStepByIndex(sessionId, stepIndex);
        if (step == null) {
            throw new RuntimeException("步骤不存在");
        }
        
        step.setStatus(StepStatus.MASTERED.getValue());
        step.setMasteredAt(LocalDateTime.now());
        step.setUpdatedAt(LocalDateTime.now());
        learningStepMapper.updateById(step);
        
        session.setCurrentStep(stepIndex);
        baseMapper.updateById(session);
        
        QueryWrapper<LearningStep> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId);
        wrapper.eq("status", StepStatus.MASTERED.getValue());
        long masteredCount = learningStepMapper.selectCount(wrapper);
        
        boolean isCompleted = masteredCount >= session.getTotalSteps();
        
        if (isCompleted) {
            session.setStatus(SessionStatus.COMPLETED.getValue());
            session.setCompletedAt(LocalDateTime.now());
            baseMapper.updateById(session);
            generateSummary(session);
        }
        
        MasterResponse.NextStepInfo nextStep = null;
        if (!isCompleted && stepIndex < session.getTotalSteps()) {
            LearningStep nextStepEntity = getStepByIndex(sessionId, stepIndex + 1);
            if (nextStepEntity != null) {
                nextStep = new MasterResponse.NextStepInfo(
                        nextStepEntity.getStepIndex(),
                        nextStepEntity.getTitle()
                );
            }
        }
        
        log.info("标记步骤为已学会: sessionId={}, stepIndex={}", sessionId, stepIndex);
        
        return new MasterResponse(StepStatus.MASTERED.getValue(), nextStep, isCompleted);
    }

    @Override
    public String getStepTeachingContent(String sessionId, Integer stepIndex) {
        LearningSession session = getSessionById(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }
        
        LearningStep step = getStepByIndex(sessionId, stepIndex);
        if (step == null) {
            throw new RuntimeException("步骤不存在");
        }
        
        String content = step.getContent();
        if (content == null || content.isEmpty()) {
            content = "## " + step.getTitle() + "\n\n暂无详细讲解内容，请继续学习。";
        }
        
        return content;
    }

    @Override
    public String getStepInfoForTeaching(String sessionId, Integer stepIndex) {
        LearningSession session = getSessionById(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }

        LearningStep step = getStepByIndex(sessionId, stepIndex);
        if (step == null) {
            throw new RuntimeException("步骤不存在");
        }

        StringBuilder info = new StringBuilder();
        info.append("【原始问题】\n").append(session.getQuestion()).append("\n\n");
        info.append("【当前步骤】\n");
        info.append("标题：").append(step.getTitle()).append("\n");

        String content = step.getContent();
        if (content != null && !content.isEmpty()) {
            info.append("内容：\n").append(content).append("\n");
        }

        if (stepIndex > 1) {
            LearningStep prevStep = getStepByIndex(sessionId, stepIndex - 1);
            if (prevStep != null) {
                info.append("\n【上一步概要】\n");
                info.append(prevStep.getTitle()).append("\n");
            }
        }

        return info.toString();
    }

    @Override
    public String getQuestion(String sessionId) {
        LearningSession session = getSessionById(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }
        return session.getQuestion();
    }

    @Override
    public Map<String, Object> answerUserQuestion(String sessionId, Integer stepIndex, String userQuestion) {
        LearningSession session = getSessionById(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }
        
        LearningStep step = getStepByIndex(sessionId, stepIndex);
        if (step == null) {
            throw new RuntimeException("步骤不存在");
        }
        
        String answer = aiProxyService.generateAnswer(
                userQuestion,
                step.getTitle(),
                step.getContent()
        );
        
        Map<String, Object> result = new HashMap<>();
        result.put("answer", answer);
        result.put("suggestNextAction", "ask_again");

        return result;
    }

    @Override
    public String buildAnswerPrompt(String sessionId, Integer stepIndex, String userQuestion) {
        LearningSession session = getSessionById(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }

        LearningStep step = getStepByIndex(sessionId, stepIndex);
        if (step == null) {
            throw new RuntimeException("步骤不存在");
        }

        return String.format("""
            你是一位耐心的数学老师，擅长通过画图和具体例子帮助学生理解。

            用户问题：%s

            当前知识点：
            标题：%s
            内容：%s

            【重要要求】
            1. 图文结合，图是最重要的！能用图说明的尽量用图
            2. 回答结构要清晰：
               - 【解答】简洁的解题过程，像写在答题卷上那样，不啰嗦
               - 【解析】详细的分析，包含：
                 * 题目考点分析
                 * 解题思路和技巧
                 * 注意事项和常见错误
               - 【图示】相关图形，放在最后
            3. 【解答】部分要简洁，可以直接写在答题卷上
            4. 【解析】部分用来补充说明、讲解知识点
            5. 【PLOT】或【PLOT3D】标签块放在最后，紧跟在【图示】部分

            【PLOT 二维图形协议】
            当需要绘制二维函数图像时，使用以下格式：

            【PLOT】
            expr: y = e^x
            xMin: -0.5
            xMax: 1.5
            yMin: -0.5
            yMax: 3
            bounds: y=0, x=1
            points: O(0,0), A(1,2.718)
            【END】

            【PLOT3D 三维图形协议】
            当需要绘制三维图形（向量、点、平面、旋转体）时，使用以下格式：

            【PLOT3D】
            {
              "vectors": [{"name": "a", "x": 1, "y": 2, "z": 3}],
              "points": [{"name": "P", "x": 0, "y": 1, "z": 0}],
              "planes": ["z = x + y"],
              "solids":[
                {"type":"revolution","axis":"x","expr":"x^2","xMin":0,"xMax":1,"stepsX":50,"stepsTheta":96,"opacity":0.22,"color":"#555555","caps":true,"showGuides":true,"guideColor":"#444444","guideSteps":60}
              ]
            }
            【END】

            重要提示：
            - 【PLOT3D】必须使用严格的JSON格式
            - 【PLOT】和【PLOT3D】可以同时使用

            请直接返回Markdown格式的回答内容，不要添加任何额外说明。
            """,
            userQuestion,
            step.getTitle(),
            step.getContent() != null ? step.getContent() : "");
    }

    @Override
    public int getCurrentRound(String sessionId) {
        return roundCounters.getOrDefault(sessionId, 0);
    }

    @Override
    public void recordAnswer(String sessionId, String answer, String question) {
        List<Map<String, String>> history = conversationHistories.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>());
        Map<String, String> entry = new HashMap<>();
        entry.put("role", "student");
        entry.put("content", answer);
        history.add(entry);
        roundCounters.merge(sessionId, 1, Integer::sum);
        log.info("记录用户回答: sessionId={}, round={}, answer={}", sessionId, getCurrentRound(sessionId), answer);
    }

    @Override
    public void appendTeacherMessage(String sessionId, String content) {
        List<Map<String, String>> history = conversationHistories.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>());
        Map<String, String> entry = new HashMap<>();
        entry.put("role", "teacher");
        entry.put("content", content);
        history.add(entry);

        int round = roundCounters.getOrDefault(sessionId, 0);
        completedModules.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>())
                .add(MODULE_NAMES[Math.min(round, MODULE_NAMES.length - 1)]);
    }

    @Override
    public String buildTeachPrompt(String sessionId, String question) {
        int round = getCurrentRound(sessionId);
        List<Map<String, String>> history = conversationHistories.getOrDefault(sessionId, new ArrayList<>());

        StringBuilder prompt = new StringBuilder();
        prompt.append(buildTeachPromptHeader(question));

        if (round == 0) {
            prompt.append(buildRoundZeroTask());
        } else {
            prompt.append(buildRoundNTask(sessionId, history, round));
        }

        prompt.append(OUTPUT_FORMAT_GUIDE);
        return prompt.toString();
    }

    private String buildTeachPromptHeader(String question) {
        return "# 角色设定\n" +
               "你是一位经验丰富的金牌数学老师，擅长一对一互动教学。\n" +
               "你要以图文结合的方式讲解，图是最重要的！\n\n" +
               "# 题目\n" + question + "\n\n";
    }

    private String buildRoundZeroTask() {
        return "# 当前任务\n" +
               "这是第一轮教学，请只讲「**" + MODULE_NAMES[0] + "」**。\n\n" +
               MODULE_DESCRIPTIONS[0] + "\n\n" +
               "**⚠️ 规则：\n" +
               "1. 只讲第一个模块的内容\n" +
               "2. 讲解要图文结合，图是最重要的！能用图说明的尽量用图\n" +
               "3. 讲解结构要清晰：\n" +
               "   - 【解答】简洁的解题过程，像写在答题卷上那样\n" +
               "   - 【解析】详细的考点分析和技巧说明\n" +
               "   - 【图示】相关图形，放在最后\n" +
               "4. 【PLOT】标签放在最后，紧跟在【图示】部分\n" +
               "5. 讲完后用【提问】询问用户是否理解\n";
    }

    private String buildRoundNTask(String sessionId, List<Map<String, String>> history, int round) {
        List<String> done = completedModules.getOrDefault(sessionId, new ArrayList<>());
        int moduleIndex = Math.min(round, MODULE_NAMES.length - 1);

        StringBuilder sb = new StringBuilder();
        sb.append("# 已完成的教学内容\n");
        for (int i = 0; i < done.size() && i < MODULE_NAMES.length; i++) {
            sb.append("- 【已完成】").append(done.get(i)).append("\n");
        }
        sb.append("\n");

        sb.append("# 学生的最新反馈\n");
        Map<String, String> lastStudentMsg = null;
        for (int i = history.size() - 1; i >= 0; i--) {
            if ("student".equals(history.get(i).get("role"))) {
                lastStudentMsg = history.get(i);
                break;
            }
        }
        if (lastStudentMsg != null) {
            sb.append("学生回复了：").append(lastStudentMsg.get("content")).append("\n\n");
        } else {
            sb.append("学生表示已经理解了前面的内容。\n\n");
        }

        sb.append("# 当前任务\n");
        sb.append("**现在开始讲第").append(round + 1).append("个模块：").append(MODULE_NAMES[moduleIndex]).append("**\n\n");
        sb.append(MODULE_DESCRIPTIONS[moduleIndex]).append("\n\n");
        sb.append("**⚠️ 极其重要的规则（必须遵守）：\n");
        sb.append("1. 以上【已完成】的模块已经全部讲过，学生已经理解了\n");
        sb.append("2. 绝对不要重复已完成的模块内容，直接从当前模块开始讲\n");
        sb.append("3. 基于学生的最新反馈决定讲解方式\n");
        sb.append("4. 讲解要图文结合，图是最重要的！能用图说明的尽量用图\n");
        sb.append("5. 讲解结构要清晰：\n");
        sb.append("   - 【解答】简洁的解题过程，像写在答题卷上那样\n");
        sb.append("   - 【解析】详细的考点分析和技巧说明\n");
        sb.append("   - 【图示】相关图形，放在最后\n");
        sb.append("6. 【PLOT】标签放在最后，紧跟在【图示】部分\n");
        sb.append("7. 讲完后用【提问】询问用户是否理解\n");

        if (round >= MODULE_NAMES.length - 1) {
            sb.append("8. 这是最后一个模块，讲完后直接做总结即可，不需要用【提问】\n");
        }
        return sb.toString();
    }

    private static final String OUTPUT_FORMAT_GUIDE = """

        ## 输出格式要求（极其重要）

        1. 先用 Markdown 格式编写**所有**讲解内容
        2. 【PLOT】标签块**必须放在回答的最最最后**，整个回答只有一个【PLOT】块，且在所有文字内容之后

        ### ✅ 正确示例（必须这样做）：

        ```
        ### 一、函数性质
        内容...

        ### 二、图像特征
        内容...

        （这里可以添加更多讲解内容...）

        【PLOT】   ← ✅ 只有这里可以放【PLOT】，且之前的所有讲解都已结束
        expr: y = sin(x)
        xMin: -6.5
        xMax: 6.5
        yMin: -1.2
        yMax: 1.2
        bounds: y=1, y=-1
        points: O(0,0), A(1.5708,1), B(-1.5708,-1)
        【END】
        ```

        ## 函数名规范（极其重要 — 前端直接使用 math.js 解析）

        【PLOT】中的 expr 表达式必须使用 math.js 兼容的函数名，前端无需任何转换即可直接传入 math.js。

        | 数学含义 | ✅ 正确写法 | ❌ 错误写法 |
        |---------|-----------|-----------|
        | 正弦 | sin(x) | — |
        | 余弦 | cos(x) | — |
        | 正切 | tan(x) | — |
        | 反正弦 | asin(x) | arcsin(x)、sin⁻¹(x) |
        | 反余弦 | acos(x) | arccos(x)、cos⁻¹(x) |
        | 反正切 | atan(x) | arctan(x)、tan⁻¹(x) |
        | 自然对数 | log(x) | ln(x) |
        | 常用对数(底10) | log10(x) | lg(x) |
        | 平方根 | sqrt(x) | √x |
        | 绝对值 | abs(x) | |x| |

        - ❌ 禁止使用 arcsin/arccos/arctan/ln/lg 等非 math.js 函数名
        - ✅ expr 必须使用上表"正确写法"列中的函数名

        ## 图像配置格式

        【PLOT】必须严格按照以下格式返回，每个字段单独一行，字段名和值用冒号分隔。

        ### 1. 普通函数（最常用）

        ```
        【PLOT】
        expr: y = sin(x)
        xMin: -6.5
        xMax: 6.5
        yMin: -1.2
        yMax: 1.2
        bounds: y=1, y=-1
        points: O(0,0), A(1.5708,1), B(-1.5708,-1)
        【END】
        ```

        ### 2. 定积分区域图形（用于求面积/定积分题目）

        当题目涉及求曲线与坐标轴围成面积时，必须绘制阴影填充区域：

        ```
        【PLOT】
        expr: y = x^2
        xMin: -0.5
        xMax: 1.5
        yMin: -0.5
        yMax: 1.5
        bounds: y=0, x=1
        points: O(0,0), A(1,1)
        【END】
        ```

        **必须包含的内容：**
        1. 曲线：`expr: y = ...`
        2. 下边界：`bounds: y=0`（x轴）
        3. 右边界：`bounds: x=1`（x=1的垂直线）
        4. 左边界：`x=0`（y轴，通过 xMin=0 实现）
        5. points 至少包含：`O(0,0)` 和 `A(1,f(1))`
        6. **必须设置 `fill: true`** 来启用阴影填充

        ### 3. 参数方程

        ```
        【PLOT】
        type: parametric
        xExpr: 2*cos(t)
        yExpr: 3*sin(t)
        tMin: 0
        tMax: 6.2832
        xMin: -2.5
        xMax: 2.5
        yMin: -3.5
        yMax: 3.5
        points: A(2,0), B(-2,0), C(0,3), D(0,-3)
        【END】
        ```

        ### 4. 极坐标方程

        ```
        【PLOT】
        type: polar
        rExpr: 1 + cos(theta)
        thetaMin: 0
        thetaMax: 6.2832
        xMin: -3
        xMax: 3
        yMin: -2
        yMax: 2
        【END】
        ```

        ### 5. 分段函数

        ```
        【PLOT】
        type: piecewise
        expr: x, x >= 0
        expr: -x, x < 0
        xMin: -3
        xMax: 3
        yMin: -1
        yMax: 3
        【END】
        ```

        ## 字段说明

        ### 通用字段

        1. **type**: 图像类型（可选，默认为 function）
           - `function`: 普通函数（默认）
           - `parametric`: 参数方程
           - `polar`: 极坐标方程
           - `piecewise`: 分段函数
        2. **xMin/xMax**: x轴范围（可选）
        3. **yMin/yMax**: y轴范围（可选）
        4. **points**: 关键点，格式 大写字母(x,y)，原点用 O 表示（可选）
        5. **asymptotes**: 渐近线列表（可选），如 x=±1.5708（垂直渐近线）、y=±1.5708（水平渐近线）
        6. **bounds**: 值域边界列表（可选），如 y=1, y=-1（函数可以达到的最大/最小值），也可用于指定 x 的边界如 x=1
        7. **fill**: 是否填充区域（可选），设为 true 时对 bounds 围成的区域进行阴影填充

        ### 普通函数专用字段

        8. **expr**: 函数表达式（必填），如 y = sin(x), y = atan(x)，必须使用 math.js 函数名

        ### 参数方程专用字段

        5. **xExpr**: x关于参数t的表达式，如 2*cos(t)
        6. **yExpr**: y关于参数t的表达式，如 3*sin(t)
        7. **tMin/tMax**: 参数t的取值范围

        ### 极坐标方程专用字段

        5. **rExpr**: 极径r关于角度theta的表达式，如 1 + cos(theta)
        6. **thetaMin/thetaMax**: 角度theta的取值范围

        ### 分段函数专用字段

        5. **expr**: 分段表达式，每行一个，如 x, x >= 0

        ## 点标签规范

        - 原点必须使用大写字母 O（Origin）
        - 其他点使用大写字母 A, B, C...
        - 奇函数：O + 正负对称点（如 atan, asin, sin, tan）
        - 偶函数：O 或顶点 + 两侧对称点（如 x^2, cos）
        - 周期函数：最多5个点

        ## 点标签关联（极其重要 — 让文本与图像保持一致）

        在讲解文本中描述特殊点时，**必须明确标注点标签**，与【PLOT】块中的 points 字段保持一致。这样用户才能将文本中的描述与图像上的点对应起来。

        ### 正确示例（文本明确标注点标签）：

        ```markdown
        ### 三、关键点
        - **端点A(-1, 3.1416)**：当x=-1时，函数值为π≈3.1416；
        - **原点O(0, 1.5708)**：当x=0时，函数值为π/2≈1.5708；
        - **端点B(1, 0)**：当x=1时，函数值为0。

        【PLOT】
        ...
        points: A(-1,3.1416), O(0,1.5708), B(1,0)
        【END】
        ```

        ### 关联规则：

        1. **每个关键点都要标注标签**：文本中描述的关键点必须带上【PLOT】中相同的标签（A/B/C/O）
        2. **格式统一**：使用 **标签(坐标)** 格式，如 **A(1, 1.5708)**
        3. **在适当时机引用标签**：在讲解单调性、极值、对称性时引用相关点标签
        4. **与【PLOT】保持一致**：文本中出现的点标签必须出现在 points 字段中
        """;

    @Override
    public String buildSummaryPrompt(String sessionId) {
        LearningSession session = getSessionById(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }

        QueryWrapper<LearningStep> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId);
        wrapper.eq("status", StepStatus.MASTERED.getValue());
        long masteredCount = learningStepMapper.selectCount(wrapper);

        String totalTime = calculateTotalTime(session.getCreatedAt(), session.getCompletedAt());

        Map<String, String> variables = new HashMap<>();
        variables.put("question", session.getQuestion());
        variables.put("completedSteps", String.valueOf(masteredCount));
        variables.put("totalTime", totalTime);

        return promptTemplateManager.renderWithMap("SUMMARY", variables);
    }

    @Override
    public void clearHistory(String sessionId) {
        conversationHistories.remove(sessionId);
        roundCounters.remove(sessionId);
        completedModules.remove(sessionId);
        log.info("清除对话历史: sessionId={}", sessionId);
    }

    private LearningSession getSessionById(String sessionId) {
        QueryWrapper<LearningSession> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId);
        return baseMapper.selectOne(wrapper);
    }

    private LearningStep getStepByIndex(String sessionId, Integer stepIndex) {
        QueryWrapper<LearningStep> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId);
        wrapper.eq("step_index", stepIndex);
        return learningStepMapper.selectOne(wrapper);
    }

    /**
     * 清理AI响应，提取纯JSON内容
     * 移除Markdown格式包裹（如 ```json, ``` 等）
     */
    private String cleanJsonResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return response;
        }

        String cleaned = response.trim();

        // 移除开头的 ```json 或 ```
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        // 移除结尾的 ```
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        // 再次清理前后空白
        return cleaned.trim();
    }
}
