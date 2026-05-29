package com.suiyuan.iragent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suiyuan.iragent.config.RedisChatMemoryRepository;
import com.suiyuan.iragent.config.VolcEngineStreamingClient;
import com.suiyuan.iragent.dto.Plot3DConfig;
import com.suiyuan.iragent.dto.PlotConfig;
import com.suiyuan.iragent.dto.ResponseSegment;
import com.suiyuan.iragent.rag.retrieval.PersonalNoteRetriever;
import com.suiyuan.iragent.rag.retrieval.PersonalNoteRetriever.NoteFragment;
import com.suiyuan.iragent.service.ConversationService;
import com.suiyuan.iragent.service.IntentRouterService;
import com.suiyuan.iragent.service.NoteAnchoredChatService;
import com.suiyuan.iragent.utils.ContentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteAnchoredChatServiceImpl implements NoteAnchoredChatService {

    private final VolcEngineStreamingClient streamingClient;
    private final PersonalNoteRetriever noteRetriever;
    private final IntentRouterService intentRouter;
    private final ObjectMapper objectMapper;
    private final RedisChatMemoryRepository memoryRepository;
    private final ConversationService conversationService;

    public void chat(long userId, String conversationId, String question, Consumer<String> onChunk,
                      Consumer<List<Map<String, Object>>> onNoteRefs,
                      Consumer<PlotConfig> onPlot, Consumer<Plot3DConfig> onPlot3d,
                      Runnable onComplete, Consumer<Throwable> onError) {

        IntentRouterService.Intent intent = intentRouter.detect(question);
        Map<String, Object> modeConfig = intentRouter.getModeConfig(intent);

        // 1. 检索用户笔记
        List<NoteFragment> fragments = noteRetriever.search(userId, question, 3);

        // 2. 获取对话历史
        List<String> history = conversationId != null && !conversationId.isEmpty()
                ? memoryRepository.get(conversationId, 10)
                : List.of();

        // 3. 构建 System Prompt（含历史对话）
        String systemPrompt = buildSystemPrompt(intent, modeConfig, fragments, history);

        // 4. 构建完整 Prompt
        String fullPrompt = systemPrompt + "\n\n---\n用户问题：\n" + question +
                "\n\n⚠️ 重要：PLOT/PLOT3D块前面绝对不能加 ### 或任何Markdown标题，直接输出块内容。";

        final String cid = conversationId != null ? conversationId : "v3-" + UUID.randomUUID().toString().substring(0, 12);

        // 5. 流式调用 LLM
        StringBuilder fullResponse = new StringBuilder();
        streamingClient.streamChat(
                fullPrompt,
                text -> {
                    fullResponse.append(text);
                    onChunk.accept(text);
                },
                () -> {
                    String response = fullResponse.toString();
                    // 解析 plot 图形块
                    List<ResponseSegment> segments = ContentParser.parse(response);
                    for (ResponseSegment seg : segments) {
                        if (seg.isPlot() && seg.getPlotConfiguration() != null) {
                            onPlot.accept(seg.getPlotConfiguration());
                        } else if (seg.isPlot3d() && seg.getPlot3DConfiguration() != null) {
                            onPlot3d.accept(seg.getPlot3DConfiguration());
                        }
                    }
                    // 保存到 DB
                    try {
                        // 确保 conversation 存在（使用实际 DB ID，匹配 MyBatis-Plus ASSIGN_UUID 策略）
                        String dbCid = cid;
                        try {
                            conversationService.sendMessage(dbCid, "user", question, "text");
                        } catch (Exception e) {
                            var conv = conversationService.createConversation(userId,
                                    truncate(question, 50), "V3 智能答疑");
                            dbCid = conv.getConversationId();
                            conversationService.sendMessage(dbCid, "user", question, "text");
                        }
                        conversationService.sendMessage(dbCid, "ai", truncate(response, 5000), "text");
                    } catch (Exception e) {
                        log.error("保存聊天记录到 DB 失败: {}", e.getMessage());
                    }
                    // 保存对话历史到 Redis
                    memoryRepository.add(cid, "Q: " + question);
                    memoryRepository.add(cid, "A: " + truncate(response, 2000));
                    List<Map<String, Object>> refs = extractNoteRefs(response, fragments);
                    onNoteRefs.accept(refs);
                    onComplete.run();
                },
                onError
        );
    }

    private String buildSystemPrompt(IntentRouterService.Intent intent,
                                      Map<String, Object> config,
                                      List<NoteFragment> fragments,
                                      List<String> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是 IRAgent，一位 AI 备考导师。\n");
        sb.append("当前回答模式：").append(config.get("mode")).append("\n");
        sb.append("回答风格：").append(config.get("style")).append("\n\n");
        sb.append(getPlotProtocol());

        if (!history.isEmpty()) {
            sb.append("\n## 对话历史\n");
            for (String line : history) {
                sb.append(line).append("\n");
            }
            sb.append("---\n");
        }

        if (!fragments.isEmpty()) {
            sb.append("\n## 学生的个人笔记（可引用）\n");
            for (int i = 0; i < fragments.size(); i++) {
                sb.append("笔记片段 ").append(i + 1).append("：\n");
                sb.append(fragments.get(i).content()).append("\n");
                sb.append("---\n");
            }
            sb.append("\n如果回答中引用了以上笔记，必须在回答末尾输出一行：\n");
            sb.append("noteRefs: [{\"noteFragment\":\"引用笔记原文（原样复制，不可修改）\",\"similarity\":0.92}]\n");
            sb.append("字段说明：noteFragment=被引用的笔记原文片段（必须一字不差），similarity=匹配度0~1的数字\n");
            sb.append("禁止事项：❌不要用\"relevance\"代替\"similarity\" ❌similarity不要写成文字 ❌不要Markdown标题或代码块\n");
        }

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractNoteRefs(String response, List<NoteFragment> fragments) {
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("noteRefs:\\s*(\\[.*?\\])");
            java.util.regex.Matcher m = p.matcher(response);
            if (m.find()) {
                String json = m.group(1);
                List<Map<String, Object>> refs = objectMapper.readValue(json, List.class);
                for (Map<String, Object> ref : refs) {
                    if (!ref.containsKey("similarity") && ref.containsKey("relevance")) {
                        ref.put("similarity", ref.get("relevance"));
                    }
                    if (!ref.containsKey("similarity") && ref.containsKey("reason")) {
                        ref.put("similarity", ref.get("reason"));
                    }
                    if (!ref.containsKey("similarity")) {
                        ref.put("similarity", 0.85);
                    }
                }
                return refs;
            }
            int start = response.lastIndexOf("[");
            int end = response.lastIndexOf("]");
            if (start > 0 && end > start) {
                String json = response.substring(start, end + 1);
                return objectMapper.readValue(json, List.class);
            }
        } catch (Exception e) {
            log.debug("解析 noteRefs 失败: {}", e.getMessage());
        }

        List<Map<String, Object>> refs = new ArrayList<>();
        for (NoteFragment f : fragments) {
            Map<String, Object> ref = new HashMap<>();
            ref.put("noteFragment", f.content().substring(0, Math.min(100, f.content().length())));
            ref.put("similarity", f.similarity());
            refs.add(ref);
        }
        return refs;
    }

    private String truncate(String s, int maxLen) {
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    private String getPlotProtocol() {
        return """
                ## 图形输出协议
                当回答涉及函数图像时，必须在回答末尾使用【PLOT】协议块输出图形数据。
                多条曲线必须合并到一个PLOT块中，用逗号分隔表达式。

                【PLOT】
                expr: <函数表达式，多条用逗号分隔，如 e^x, e^(-x)>
                xmin: <x轴最小值>
                xmax: <x轴最大值>
                ymin: <y轴最小值>
                ymax: <y轴最大值>
                asymptotes: <渐近线，多条用逗号分隔>
                bounds: <积分区域边界，多条用逗号分隔>
                points: <关键点，格式 A(-1,0) B(1,4)>
                【END】

                单条曲线示例：
                【PLOT】
                expr: x^2+2*x+1
                xmin: -3
                xmax: 3
                ymin: -1
                ymax: 9
                points: A(-1,0) B(1,4)
                【END】

                多条曲线（必须合并到一个PLOT块）：
                【PLOT】
                expr: e^x, e^(-x)
                xmin: -0.5
                xmax: 1.5
                ymin: -0.5
                ymax: 3
                bounds: x=0, x=1
                points: A(0,1) B(1,e) C(1,1/e)
                【END】

                三维图形用【PLOT3D】...【END】块。以下场景必须使用PLOT3D：
                - 旋转体体积（绕x轴/y轴旋转一周形成的立体）
                - 空间曲面/曲线
                - 三维几何体
                内容为一行严格JSON（禁止换行/注释/多余空格）：
                【PLOT3D】
                {"type":"surface","expr":"x^2+y^2","xMin":-2,"xMax":2,"yMin":-2,"yMax":2}
                【END】
                旋转体示例（绕x轴旋转 y=x^2, x∈[0,1]）：
                【PLOT3D】
                {"solids":[{"type":"revolution","expr":"x^2","axis":"x","xMin":0,"xMax":1}]}
                【END】

                注意：
                1. ❌禁止在PLOT/PLOT3D块前加任何Markdown标题

                ❌错误示例（绝对禁止）：
                正文结束。
                ### 2D区域图形
                【PLOT】

                ✅正确示例（必须这样写）：
                正文结束。

                【PLOT】

                2. 旋转体体积问题必须同时输出PLOT和PLOT3D，两个块紧贴在一起，中间不留文字
                3. ymin/ymax 必须用 xmin/xmax 代入每个 expr 实际计算 y 值，取最值 ±20% 余量
                4. PLOT3D必须是单行JSON，{}后直接跟【END】
                """;
    }
}
