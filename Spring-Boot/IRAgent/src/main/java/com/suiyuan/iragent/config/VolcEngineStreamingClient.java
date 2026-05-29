package com.suiyuan.iragent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
public class VolcEngineStreamingClient {

    private final OkHttpClient okHttpClient;
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public VolcEngineStreamingClient(String apiKey, String model, String baseUrl) {
        this.apiKey = apiKey;
        this.model = (model != null && !model.isEmpty()) ? model : "doubao-seed-1-8-251228";
        this.baseUrl = (baseUrl != null && !baseUrl.isEmpty()) ? baseUrl : "https://ark.cn-beijing.volces.com/api/v3";
        this.objectMapper = new ObjectMapper();

        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        log.info("VolcEngineStreamingClient初始化完成: model={}, baseUrl={}", this.model, this.baseUrl);
    }

    @Trace(operationName = "/llm/streamChat")
    public void streamChat(String prompt, Consumer<String> onText, Runnable onComplete, Consumer<Throwable> onError) {
        long requestStart = System.currentTimeMillis();
        AtomicBoolean firstTokenReceived = new AtomicBoolean(false);
        String questionType = detectQuestionType(prompt);

        try {
            String requestBody = buildRequestBody(prompt);
            log.info("发起流式请求: model={}, prompt长度={}", model, prompt.length());

            Request request = new Request.Builder()
                    .url(this.baseUrl + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + this.apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            Call call = okHttpClient.newCall(request);
            Response response = call.execute();

            if (!response.isSuccessful() || response.body() == null) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                log.error("请求失败: code={}, body={}", response.code(), errorBody);
                throw new IOException("请求失败: " + response.code() + " - " + errorBody);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String json = line.substring(6).trim();
                    if ("[DONE]".equals(json)) {
                        continue;
                    }

                    String content = extractChatDelta(json);
                    if (content != null && !content.isEmpty()) {
                        // FTT 首字延迟采集（面试点：SSE 流式响应的特殊性）
                        if (firstTokenReceived.compareAndSet(false, true)) {
                            long ftt = System.currentTimeMillis() - requestStart;
                            ActiveSpan.tag("ftt_ms", String.valueOf(ftt));
                            ActiveSpan.tag("ftt", "true");
                            ActiveSpan.tag("question_type", questionType);
                            log.info("FTT 首字延迟: {}ms, questionType={}", ftt, questionType);
                        }
                        onText.accept(content);
                    }
                }
            }

            reader.close();
            // 总流式耗时
            ActiveSpan.tag("total_stream_ms", String.valueOf(System.currentTimeMillis() - requestStart));
            onComplete.run();

        } catch (Exception e) {
            log.error("流式请求异常", e);
            ActiveSpan.error(e);
            onError.accept(e);
        }
    }

    private String detectQuestionType(String prompt) {
        if (prompt == null) return "unknown";
        if (prompt.contains("数学") || prompt.contains("函数") || prompt.contains("方程") || prompt.contains("几何")) return "math";
        if (prompt.contains("物理") || prompt.contains("力学") || prompt.contains("电路")) return "physics";
        if (prompt.contains("化学") || prompt.contains("反应") || prompt.contains("元素")) return "chemistry";
        if (prompt.contains("英语") || prompt.contains("翻译") || prompt.contains("语法")) return "english";
        return "general";
    }

    /**
     * 构建请求体 - Chat API 格式
     */
    private String buildRequestBody(String prompt) {
        return buildRequestBody(prompt, null);
    }

    private String buildRequestBody(String prompt, String base64Image) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("model", model);
            request.put("stream", true);

            Map<String, String> thinking = new HashMap<>();
            thinking.put("type", "disabled");
            request.put("thinking", thinking);

            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");

            if (base64Image != null && !base64Image.isEmpty()) {
                // 多模态：content 数组格式 [image_url, text]
                List<Map<String, Object>> content = new ArrayList<>();
                Map<String, Object> imagePart = new HashMap<>();
                imagePart.put("type", "image_url");
                imagePart.put("image_url", Map.of("url", "data:image/jpeg;base64," + base64Image));
                content.add(imagePart);
                Map<String, Object> textPart = new HashMap<>();
                textPart.put("type", "text");
                textPart.put("text", prompt);
                content.add(textPart);
                message.put("content", content);
            } else {
                message.put("content", prompt);
            }
            messages.add(message);
            request.put("messages", messages);

            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            log.error("构建请求体失败", e);
            return "{}";
        }
    }

    /**
     * 带图片的多模态流式调用
     */
    @Trace(operationName = "/llm/streamChatWithImage")
    public void streamChatWithImage(String prompt, String base64Image,
                                     Consumer<String> onText, Runnable onComplete, Consumer<Throwable> onError) {
        try {
            String requestBody = buildRequestBody(prompt, base64Image);
            log.info("多模态请求发送: model={}, 图片大小={} bytes", model,
                    base64Image != null ? base64Image.length() : 0);

            Request request = new Request.Builder()
                    .url(this.baseUrl + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + this.apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            Call call = okHttpClient.newCall(request);
            Response response = call.execute();

            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("请求失败: " + response.code());
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6);
                    if ("[DONE]".equals(data)) { onComplete.run(); return; }
                    String content = extractChatDelta(data);
                    if (content != null) onText.accept(content);
                }
            }
            onComplete.run();
        } catch (Exception e) {
            log.error("多模态请求失败", e);
            onError.accept(e);
        }
    }

    private String extractChatDelta(String jsonStr) {
        try {
            Map<String, Object> response = objectMapper.readValue(jsonStr, Map.class);

            Object choicesObj = response.get("choices");
            if (choicesObj instanceof List) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) choicesObj;
                for (Map<String, Object> choice : choices) {
                    Object deltaObj = choice.get("delta");
                    if (deltaObj instanceof Map) {
                        Map<String, Object> delta = (Map<String, Object>) deltaObj;
                        Object content = delta.get("content");
                        if (content != null) {
                            return content.toString();
                        }
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.debug("解析响应失败: {}, 原始数据: {}", e.getMessage(), jsonStr);
            return null;
        }
    }

    /**
     * 模型预热 - 在服务启动时调用一次，减少首次请求延迟
     */
    public void warmUp() {
        try {
            log.info("开始模型预热...");
            String requestBody = buildRequestBody("你好");

            Request request = new Request.Builder()
                    .url(this.baseUrl + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + this.apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            Call call = okHttpClient.newCall(request);
            Response response = call.execute();

            if (response.isSuccessful()) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    // 读取但不处理，完成预热即可
                }
                reader.close();
                log.info("模型预热完成");
            } else {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                log.warn("模型预热失败: code={}, body={}", response.code(), errorBody);
            }
        } catch (Exception e) {
            log.warn("模型预热异常: {}", e.getMessage());
        }
    }
}
