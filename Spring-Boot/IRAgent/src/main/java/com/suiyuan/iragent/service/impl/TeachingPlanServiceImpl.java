package com.suiyuan.iragent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suiyuan.iragent.config.VolcEngineChatClient;
import com.suiyuan.iragent.entity.TeachingTimeline;
import com.suiyuan.iragent.entity.TimelineAction;
import com.suiyuan.iragent.entity.ViewBox;
import com.suiyuan.iragent.service.TeachingPlanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TeachingPlanServiceImpl implements TeachingPlanService {

    private final VolcEngineChatClient deepSeekChatClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.timeline.model:doubao-seed-1-8-251228}")
    private String model;

    @Value("${ai.timeline.enabled:true}")
    private boolean aiEnabled;

    @Autowired(required = false)
    public TeachingPlanServiceImpl(@Qualifier("deepSeekChatClient") VolcEngineChatClient deepSeekChatClient, ObjectMapper objectMapper) {
        this.deepSeekChatClient = deepSeekChatClient;
        this.objectMapper = objectMapper;
    }

    public TeachingTimeline generateTimeline(String topic) {
        if (!aiEnabled || deepSeekChatClient == null) {
            log.warn("[TeachingPlan] AI生成已禁用，返回null");
            return null;
        }

        String prompt = buildPrompt(topic);
        log.info("[TeachingPlan] 发送请求: topic={}", topic);

        try {
            String rawResponse = deepSeekChatClient.chat(prompt);
            log.debug("[TeachingPlan] 原始响应: {}", rawResponse);

            String cleanJson = cleanJsonResponse(rawResponse);
            TeachingTimeline timeline = objectMapper.readValue(cleanJson, TeachingTimeline.class);
            
            if (timeline != null && timeline.getTimeline() != null) {
                validateAndFixTimeline(timeline);
                logTimeline(timeline);
            }
            
            log.info("[TeachingPlan] 生成成功: title={}, duration={}s, actions={}",
                    timeline.getLessonTitle(), timeline.getDurationSeconds(),
                    timeline.getTimeline() != null ? timeline.getTimeline().size() : 0);
            
            return timeline;
        } catch (Exception e) {
            log.error("[TeachingPlan] 生成失败: {}", e.getMessage(), e);
            return null;
        }
    }

    private String buildPrompt(String topic) {
        return """
# Role: 高级数学讲师 & 视频脚本专家

## Task
根据用户提供的数学题目，生成一个用于黑板视频教学的Timeline JSON。

## JSON Schema (必须严格遵循)
```json
{
  "lessonTitle": "一次函数的斜率和截距",
  "topic": "已知函数 y=3x+5，求它的斜率和截距",
  "durationSeconds": 75,
  "style": "blackboard",
  "timeline": [
    {"time": 0, "action": "write_text", "text": "拿到题目，先画图像看看！", "audioTrigger": true, "duration": 3.0},
    {"time": 3, "action": "show_grid", "duration": 1.0},
    {"time": 4, "action": "draw_graph", "expr": "3*x + 5", "duration": 5.0},
    {"time": 9, "action": "annotate", "text": "截距(0,5)", "duration": 3.0},
    {"time": 12, "action": "write_text", "text": "好，接下来我们一步步解", "audioTrigger": true, "duration": 3.0},
    {"time": 15, "action": "write_text", "text": "第一步，写标准形式", "audioTrigger": true, "duration": 3.0},
    {"time": 18, "action": "write_formula", "latex": "y = kx + b", "duration": 2.0},
    {"time": 20, "action": "write_text", "text": "第二步，写已知函数", "audioTrigger": true, "duration": 3.0},
    {"time": 23, "action": "write_formula", "latex": "y = 3x + 5", "duration": 2.0},
    {"time": 25, "action": "write_text", "text": "第三步，对照找参数", "audioTrigger": true, "duration": 3.0},
    {"time": 28, "action": "write_formula", "latex": "k = 3", "duration": 2.0},
    {"time": 30, "action": "write_formula", "latex": "b = 5", "duration": 2.0},
    {"time": 32, "action": "clear_board", "duration": 1.0},
    {"time": 33, "action": "write_text", "text": "总结一下完整计算过程：", "audioTrigger": true, "duration": 3.0},
    {"time": 36, "action": "write_formula", "latex": "y = kx + b", "duration": 2.0},
    {"time": 38, "action": "write_formula", "latex": "y = 3x + 5", "duration": 2.0},
    {"time": 40, "action": "write_formula", "latex": "k = 3", "duration": 2.0},
    {"time": 42, "action": "write_formula", "latex": "b = 5", "duration": 2.0},
    {"time": 44, "action": "write_key", "text": "答案", "duration": 2.0}
  ]
}
```

## 动作类型说明
| 动作类型 | 目的 | 对应数据字段 | 约束 |
|---------|------|-------------|------|
| write_text | **带语音的文字**：显示文字并触发 TTS 朗读 | text: "口语化内容" | 用于【析】【解】【评】三阶段 |
| write_key | **重点标记**（黄色高亮）：仅用于最终结论 | text: "≤6汉字" | **整个 Timeline 最多用 3 次**，仅限结论处 |
| write_formula | 渲染 LaTeX 公式 | latex: "公式（不含$）" | |
| show_grid | 显示坐标系网格 | 无 | draw_graph 前 1 秒 |
| draw_graph | 绘制函数曲线 | expr: "函数表达式" | |
| annotate | 标注关键点 | text: "坐标或说明" | |
| clear_board | 清空黑板 | 无 | 阶段切换时 |

## 解题三部曲（必须严格遵循）

### 【析】（Analysis）- 先画再分析
**只要有函数，第一步先画图像！**
1. 使用 write_text 引导："拿到题目，先画图像看看"
2. 使用 show_grid 显示坐标系
3. 使用 draw_graph 画出函数
4. 使用 annotate 标注关键点（如截距、顶点）
5. **禁止**在此阶段使用 write_key

### 【解】（Solution）- 规范解题
解题必须包含以下"做题痕迹"：
1. **写已知**：用 write_formula 写出题目给出的函数
2. **找对应**：用 write_text 解释"对照标准式"
3. **标参数**：用 write_formula 逐步标注 k=?, b=?
4. **留过程**：每个推导步骤都要展示，不要跳步
5. **禁止**在此阶段使用 write_key

### 【评】（Conclusion）- 完整总结
**最后必须有完整计算过程总结！**
1. 用 clear_board 清屏（这是唯一合法的清屏时机）
2. 用 write_text 引导："总结一下完整计算过程"
3. 用 write_formula 依次展示：标准式 → 已知条件 → 推导过程 → 最终答案
4. 用 write_key 框出最终结论
5. **禁止**在总结后再加 clear_board

## LaTeX 规范（必须严格遵守）
1. 严禁使用 \\begin{matrix} 等复杂环境
2. 分数必须使用 \\\\frac{a}{b}，不要嵌套太深
3. 指数使用 ^，下标使用 _
4. 复杂推导必须拆分成多个 write_formula，不要堆叠
5. **latex 字段禁止包含 $ 定界符**：直接写公式内容如 "y = kx + b" 或 "\\frac{dy}{dx}"

## 时间轴约束（必须严格遵守）
1. 每个action的time = 上一个action的time + 上一个action的duration
2. 第一个action的time必须是0
3. durationSeconds = 最后一个action的time + 最后一个action的duration
4. **严禁纯语音段落超过 10 秒**：长语音必须拆分，配合视觉动作
5. timeline数组必须按time字段严格升序排列

## 教学节奏要求（核心！）
1. **遵循解题三部曲**：每道题必须按【析】→【解】→【评】结构生成
2. **图像优先！**：【析】阶段先画图像，再分析
3. **图像保留到最后！**：【析】→【解】→【评】整个过程中，图像始终保留
4. **绝对禁止：**不要在【析】和【解】之间加 clear_board！
5. **write_key 仅用于结论**：【析】和【解】阶段严禁使用，只有【评】阶段可用
6. **语音 + 视觉交替**：每段语音后紧跟视觉动作
7. **画图前 show_grid**：draw_graph 前 1 秒显示坐标系
8. **标注提升理解**：draw_graph 后用 annotate 标注关键点

## 动作比例建议
- write_text：45-55%（带语音讲解）
- write_formula：20-25%（分步推导）
- draw_graph + show_grid + annotate：20-25%（图像相关）
- write_key：≤10%（仅最终结论，最多 3 次）

## TTS 字段生成规范
**DeepSeek 必读：write_text 动作的 text 字段直接用于显示和 TTS朗读！**
1. text 字段：口语化表达，像老师在课堂讲解
   - 多用"接下来"、"先看"、"对照"等引导词
   - 体现解题思路：如"对照标准式找对应项"
2. **口语化转换规则**：
   - ^2 → 的平方
   - ^3 → 的立方
   - 变量前后加空格防止连读
3. **信息完整性**：必须包含所有关键数值

## 解题规范（重要！）
**图像优先原则**：
✅ 能画图的题目，第一步先画图像！
✅ 画图后，结合图像讲解（annotate 标注关键点）
✅ 图像 + 文字，双重理解

**推荐做题流程**：
1. 读题 → 先画图像（draw_graph）
2. 标注关键点（annotate）
3. 观察图像 → 写出已知条件
4. 找标准形式/公式
5. 对照 → 标出每个参数
6. 给出答案
7. **清屏 → 完整计算过程总结**（必须！）

**完整计算过程总结**（必须包含）：
1. 先清屏（clear_board）
2. 依次展示：标准式 → 已知函数 → 参数值 → 最终答案
3. 用 write_key 框出结论

**禁止**：
- ❌ 直接给答案，跳过推导过程
- ❌ 没有"写已知"这一步
- ❌ 跳步（如一步写出 k=3,b=5 不解释）
- ❌ 有函数不画图，直接解答
- ❌ 用 write_key 做中间步骤
- ❌ 总结后再清屏
- ❌ 【析】和【解】之间清屏（只有【评】前可以清屏）

## 坐标系适配
- X_RANGE固定为6，Y_RANGE默认为10，剧烈函数可调整到20
- 确保图像能完整显示在屏幕内
- 避免出现奇点或不合理定义域

## 输出要求
- 只输出纯JSON字符串
- 严禁任何Markdown标记（如```json）
- JSON必须能被Jackson正确解析
- 时间轴必须连续，不允许跳跃
- 每个动作的 id 必须全局唯一（格式：a0, a1, a2...）

## 用户题目
""" + topic;
    }

    private String cleanJsonResponse(String raw) {
        if (raw == null) return "{}";
        String cleaned = raw.replaceAll("(?s)<think>.*?</think>", "");
        cleaned = cleaned.replaceAll("```json", "");
        cleaned = cleaned.replaceAll("```", "").trim();
        int jsonStart = cleaned.indexOf('{');
        int jsonEnd = cleaned.lastIndexOf('}');
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            cleaned = cleaned.substring(jsonStart, jsonEnd + 1);
        }
        return cleaned.trim();
    }
    
    private void validateAndFixTimeline(TeachingTimeline timeline) {
        if (timeline.getTimeline() == null || timeline.getTimeline().isEmpty()) {
            return;
        }
        
        timeline.setTimeline(removeDuplicateShowGrid(timeline.getTimeline()));
        timeline.setTimeline(removeIntermediateClearBoard(timeline.getTimeline()));
        
        double expectedTime = 0;
        int index = 0;
        int writeKeyCount = 0;
        java.util.Set<String> usedIds = new java.util.HashSet<>();
        int lastDrawGraphIndex = -1;
        String lastActionType = null;
        
        for (var action : timeline.getTimeline()) {
            String generatedId = "a" + index;
            if (action.getId() == null || usedIds.contains(action.getId())) {
                action.setId(generatedId);
            }
            usedIds.add(action.getId());
            
            if ("speak".equals(action.getAction())) {
                action.setAction("write_text");
                log.info("[TeachingPlan] 动作类型转换: speak -> write_text, id={}", action.getId());
            }
            
            if ("write_text".equals(action.getAction())) {
                action.setAudioTrigger(true);
                String speakText = action.getText();
                if (speakText != null) {
                    int textLength = speakText.length();
                    double calculatedDuration = Math.ceil(textLength / 4.0) + 1;
                    if (calculatedDuration > 10) {
                        log.warn("[TeachingPlan] write_text 语音过长({}字={}s)，建议拆分: {}",
                                textLength, calculatedDuration, speakText);
                    }
                    action.setDuration(calculatedDuration);
                    action.setAudioDuration(calculatedDuration);
                }
            }
            
            if ("write_key".equals(action.getAction())) {
                writeKeyCount++;
                if (writeKeyCount > 3) {
                    log.warn("[TeachingPlan] write_key 超过3个，转换为 write_text: {}", action.getText());
                    action.setAction("write_text");
                    action.setAudioTrigger(true);
                }
                if (action.getText() != null && action.getText().length() > 10) {
                    action.setText(action.getText().trim().substring(0, 10));
                }
            }
            
            if ("write_formula".equals(action.getAction()) && action.getLatex() != null) {
                String latex = action.getLatex().trim();
                if (latex.contains("$")) {
                    latex = latex.replace("$", "");
                    action.setLatex(latex);
                }
            }
            
            if ("draw_graph".equals(action.getAction())) {
                lastDrawGraphIndex = index;
            }
            
            if (action.getTime() != expectedTime) {
                action.setTime(expectedTime);
            }
            expectedTime = action.getTime() + action.getDuration();
            lastActionType = action.getAction();
            index++;
        }
        
        if (lastDrawGraphIndex > 0) {
            insertShowGridBeforeDrawGraph(timeline, lastDrawGraphIndex);
        }
        
        timeline.setDurationSeconds((int)expectedTime);
        
        calculateDynamicViewBox(timeline);
    }
    
    private java.util.List<TimelineAction> removeDuplicateShowGrid(java.util.List<TimelineAction> actions) {
        java.util.List<TimelineAction> result = new java.util.ArrayList<>();
        String lastAction = null;
        for (var action : actions) {
            if ("show_grid".equals(action.getAction()) && "show_grid".equals(lastAction)) {
                log.info("[TeachingPlan] 移除重复 show_grid 动作");
                continue;
            }
            result.add(action);
            lastAction = action.getAction();
        }
        return result;
    }
    
    private java.util.List<TimelineAction> removeIntermediateClearBoard(java.util.List<TimelineAction> actions) {
        java.util.List<TimelineAction> result = new java.util.ArrayList<>();
        boolean hasDrawGraph = false;
        boolean hasWriteKey = false;
        
        for (var action : actions) {
            if ("draw_graph".equals(action.getAction())) {
                hasDrawGraph = true;
            }
            if ("write_key".equals(action.getAction())) {
                hasWriteKey = true;
            }
            if ("clear_board".equals(action.getAction())) {
                if (hasDrawGraph && !hasWriteKey) {
                    log.info("[TeachingPlan] 移除中间 clear_board，保留图像：{}", action.getId());
                    continue;
                }
            }
            result.add(action);
        }
        return result;
    }
    
    private void insertShowGridBeforeDrawGraph(TeachingTimeline timeline, int drawGraphIndex) {
        var actions = timeline.getTimeline();
        if (drawGraphIndex > 0) {
            TimelineAction drawGraphAction = actions.get(drawGraphIndex);
            double insertTime = drawGraphAction.getTime() - 1;
            if (insertTime >= 0) {
                TimelineAction showGrid = TimelineAction.builder()
                        .id("show_grid_" + System.currentTimeMillis())
                        .time(insertTime)
                        .action("show_grid")
                        .duration(1.0)
                        .build();
                actions.add(drawGraphIndex, showGrid);
                log.info("[TeachingPlan] 在 draw_graph 前插入 show_grid 动作");
                recalculateTimelineTimes(timeline);
            }
        }
    }
    
    private void recalculateTimelineTimes(TeachingTimeline timeline) {
        double expectedTime = 0;
        for (var action : timeline.getTimeline()) {
            action.setTime(expectedTime);
            expectedTime = action.getTime() + action.getDuration();
        }
    }
    
    private void calculateDynamicViewBox(TeachingTimeline timeline) {
        double maxY = 0;
        double minY = 0;
        double maxX = 6;
        
        for (var action : timeline.getTimeline()) {
            if ("draw_graph".equals(action.getAction()) && action.getExpr() != null) {
                double[] yRange = estimateYRange(action.getExpr());
                maxY = Math.max(maxY, yRange[1]);
                minY = Math.min(minY, yRange[0]);
            }
        }
        
        if (maxY > 0 || minY < 0) {
            double newYRange = Math.max(Math.abs(maxY), Math.abs(minY)) * 1.5;
            newYRange = Math.max(newYRange, 6);
            
            if (timeline.getViewBox() == null) {
                timeline.setViewBox(ViewBox.builder().xRange(maxX).yRange(newYRange).build());
            } else {
                timeline.getViewBox().setYRange(newYRange);
                timeline.getViewBox().setXRange(maxX);
            }
            log.info("[TeachingPlan] 动态 ViewBox: xRange={}, yRange={}", maxX, newYRange);
        }
    }
    
    private double[] estimateYRange(String expr) {
        double[] result = {0, 0};
        if (expr == null) return result;
        
        expr = expr.replace(" ", "");
        
        if (expr.contains("*") || expr.contains("x")) {
            result[1] = 5;
            result[0] = -1;
            
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([+-]?\\d+)\\*x");
            java.util.regex.Matcher matcher = pattern.matcher(expr);
            if (matcher.find()) {
                int coeff = Integer.parseInt(matcher.group(1));
                result[1] = Math.max(result[1], Math.abs(coeff) * 2);
            }
            
            pattern = java.util.regex.Pattern.compile("\\+(\\d+)(?!\\*)");
            matcher = pattern.matcher(expr);
            while (matcher.find()) {
                int val = Integer.parseInt(matcher.group(1));
                result[1] = Math.max(result[1], val + 2);
            }
        }
        
        return result;
    }

    private void logTimeline(TeachingTimeline timeline) {
        log.info("========== Timeline 输出 ==========");
        log.info("标题: {}", timeline.getLessonTitle());
        log.info("题目: {}", timeline.getTopic());
        log.info("总时长: {}s", timeline.getDurationSeconds());
        log.info("动作数量: {}", timeline.getTimeline().size());
        log.info("---------- 动作详情 ----------");
        
        for (int i = 0; i < timeline.getTimeline().size(); i++) {
            TimelineAction action = timeline.getTimeline().get(i);
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("[%02d] id=%s, time=%.1f, duration=%.1f, action=%s",
                    i, action.getId(), action.getTime(), action.getDuration(), action.getAction()));
            
            if (action.getText() != null) {
                sb.append(", text=\"").append(action.getText()).append("\"");
            }
            if (action.getLatex() != null) {
                sb.append(", latex=\"").append(action.getLatex()).append("\"");
            }
            if (action.getExpr() != null) {
                sb.append(", expr=\"").append(action.getExpr()).append("\"");
            }
            if (action.getAudioTrigger() != null && action.getAudioTrigger()) {
                sb.append(", audioTrigger=true");
            }
            
            log.info(sb.toString());
        }
        log.info("=================================");
    }
}
