package com.suiyuan.iragent.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suiyuan.iragent.config.VolcEngineStreamingClient;
import com.suiyuan.iragent.service.SmartPaperStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmartPaperStreamServiceImpl implements SmartPaperStreamService {

    private final VolcEngineStreamingClient streamingClient;
    private final ObjectMapper objectMapper;

    public void streamGeneratePaper(long userId, String prompt,
                                      Consumer<String> onChunk,
                                      Consumer<Map<String, Object>> onComplete,
                                      Consumer<Throwable> onError) {
        String fullPrompt = buildSystemPrompt() + "\n\n### 用户需求\n" + prompt;

        StringBuilder fullText = new StringBuilder();
        final boolean[] jsonBlockStarted = {false};

        streamingClient.streamChat(
                fullPrompt,
                text -> {
                    fullText.append(text);
                    if (jsonBlockStarted[0]) return;
                    String acc = fullText.toString();
                    int ji = acc.indexOf("```json");
                    if (ji >= 0) {
                        jsonBlockStarted[0] = true;
                        int chunkJi = text.indexOf("```json");
                        if (chunkJi > 0) {
                            onChunk.accept(text.substring(0, chunkJi));
                        }
                        return;
                    }
                    onChunk.accept(text);
                },
                () -> {
                    try {
                        String raw = fullText.toString();
                        int ji = raw.indexOf("```json");
                        if (ji >= 0) raw = raw.substring(0, ji).trim();
                        if (raw.isBlank()) {
                            onError.accept(new RuntimeException("AI 返回为空，请检查 API Key 和模型配置"));
                            return;
                        }
                        Map<String, Object> result = buildResult(fullText.toString());
                        result.putIfAbsent("paperId", "sp-" + UUID.randomUUID().toString().substring(0, 12));
                        result.putIfAbsent("title", "AI 智能组卷");
                        result.putIfAbsent("subject", "通用");
                        result.putIfAbsent("questionCount", 0);
                        result.putIfAbsent("totalScore", 100);
                        result.putIfAbsent("estimatedTime", 30);
                        result.putIfAbsent("difficulty", 3);
                        onComplete.accept(result);
                    } catch (Exception e) {
                        log.error("解析试卷结果失败", e);
                        onError.accept(e);
                    }
                },
                onError
        );
    }

    private String buildSystemPrompt() {
        return """
你是一位资深命题专家。根据用户的需求生成一套试卷。

输出格式要求（重要）：
1. 首先用一段话简要说明试卷概要（科目、难度、题量）。
2. 输出试卷正文，按照题型分类，使用 Markdown 格式逐题输出，不包含答案和解析：
   - 用 `# 试卷标题` 作为第一行
   - 用 `## 一、单选题` / `## 二、多选题` 等作为题型标题
   - 用 `---` 分隔不同大题
   - 每道题用 `###` 作为题目标题，格式：`### 1. 题干内容`
   - 选项用列表格式：
     - A. 选项内容
     - B. 选项内容
     - C. 选项内容
     - D. 选项内容
   - **不要在试卷正文中输出答案和解析**
3. 试卷正文结束后，单独一行输出分隔符：`---ANSWER_BREAK---`
4. 分隔符之后输出 `## 参考答案与解析`，包含每道题的正确答案和详细解析。
5. 最后（最末尾）输出一个 JSON 代码块用于结构化数据提取。
6. 数学公式请用 `$...$` 包裹行内公式，用 `$$...$$` 包裹独立公式。

JSON 格式如下（用 ```json 和 ``` 包裹）：
```json
{
  "subject": "科目",
  "examType": "考试类型",
  "title": "试卷标题",
  "questionCount": 总数,
  "difficulty": 3,
  "totalScore": 100,
  "estimatedTime": 30,
  "questions": [
    {
      "index": 1,
      "questionText": "题干",
      "questionType": "single_choice",
      "options": ["A. 选项A", "B. 选项B", "C. 选项C", "D. 选项D"],
      "correctAnswer": "A",
      "explanation": "解析",
      "difficulty": 3,
      "knowledgePoint": "知识点",
      "score": 10,
      "tags": []
    }
  ]
}
```

注意：JSON 必须严格合法，不要包含注释，特殊字符需转义。""";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildResult(String fullText) {
        Map<String, Object> meta = extractJson(fullText);
        if (meta == null) {
            log.warn("无法从 AI 输出中提取 JSON，尝试使用完整输出");
            meta = new HashMap<>();
            meta.put("title", "AI 智能组卷");
            meta.put("subject", "通用");
            meta.put("questions", new ArrayList<>());
        }

        List<Map<String, Object>> questions = (List<Map<String, Object>>) meta.getOrDefault("questions", new ArrayList<>());
        List<Map<String, Object>> validQuestions = new ArrayList<>();
        for (Map<String, Object> q : questions) {
            if (q.get("questionText") != null && !q.get("questionText").toString().isBlank()) {
                validQuestions.add(q);
            }
        }

        int totalScore = meta.get("totalScore") instanceof Number n ? n.intValue() : 100;
        int scorePerQuestion = validQuestions.isEmpty() ? 0 : totalScore / Math.max(validQuestions.size(), 1);
        String paperId = "sp-" + UUID.randomUUID().toString().substring(0, 12);

        for (int i = 0; i < validQuestions.size(); i++) {
            Map<String, Object> q = validQuestions.get(i);
            q.putIfAbsent("id", "aiq-" + UUID.randomUUID().toString().substring(0, 12));
            q.putIfAbsent("index", i + 1);
            q.putIfAbsent("score", scorePerQuestion);
            q.putIfAbsent("source", "ai-generated");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paperId", paperId);
        result.put("title", meta.getOrDefault("title", "AI 智能组卷"));
        result.put("subject", meta.getOrDefault("subject", "通用"));
        result.put("examType", meta.getOrDefault("examType", "综合"));
        result.put("totalScore", totalScore);
        result.put("questionCount", validQuestions.size());
        result.put("estimatedTime", meta.getOrDefault("estimatedTime", 30));
        result.put("difficulty", meta.getOrDefault("difficulty", 3));
        result.put("questions", validQuestions);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractJson(String text) {
        Pattern jsonBlock = Pattern.compile("```(?:json)?\\s*\\n?([\\s\\S]*?)```", Pattern.MULTILINE);
        Matcher matcher = jsonBlock.matcher(text);
        while (matcher.find()) {
            String candidate = matcher.group(1).trim();
            try {
                Map<String, Object> parsed = objectMapper.readValue(candidate, new TypeReference<>() {});
                if (parsed.containsKey("questions") || parsed.containsKey("subject")) {
                    return parsed;
                }
                } catch (Exception e) {
                    log.warn("JSON 块提取失败(candidate): {}", candidate, e);
                }
            }

            // 搜索 JSON 特征开头，避免匹配 LaTeX 花括号（如 ${x \to 0}$）
            int braceStart = text.indexOf("{\"subject\"");
            if (braceStart < 0) braceStart = text.indexOf("{\"questions\"");
            if (braceStart < 0) braceStart = text.indexOf("{\"paperId\"");
            if (braceStart < 0) braceStart = text.indexOf("{\"title\"");
        if (braceStart >= 0) {
            int depth = 0;
            int braceEnd = -1;
            for (int i = braceStart; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) { braceEnd = i + 1; break; }
                }
            }
            if (braceEnd > braceStart) {
                String candidate = text.substring(braceStart, braceEnd);
                try {
                    Map<String, Object> parsed = objectMapper.readValue(candidate, new TypeReference<>() {});
                    if (parsed.containsKey("questions") || parsed.containsKey("subject")) {
                        return parsed;
                    }
                    } catch (Exception e) {
                        log.warn("JSON 提取失败(brace): {}", candidate, e);
                    }
                }
            }

            return null;
    }
}
