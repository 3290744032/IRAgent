package com.suiyuan.iragent.rag.embedding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suiyuan.iragent.config.AIConfiguration;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class VolcengineEmbeddingClient implements EmbeddingService {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AIConfiguration aiConfig;
    private final StringRedisTemplate redis;
    private final String baseUrl;
    private final String model;

    public VolcengineEmbeddingClient(
            AIConfiguration aiConfig,
            StringRedisTemplate redis,
            @Value("${spring.ai.volcengine.base-url}") String baseUrl,
            @Value("${spring.ai.volcengine.embedding-model:doubao-embedding-vision-250615}") String embeddingModel) {
        this.aiConfig = aiConfig;
        this.redis = redis;
        this.baseUrl = baseUrl;
        this.model = embeddingModel;
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        log.info("VolcengineEmbeddingClient 初始化: model={}, baseUrl={}", model, baseUrl);
    }

    @Override
    public float[] embed(String text) {
        List<float[]> results = embedBatch(List.of(text));
        return results.isEmpty() ? new float[0] : results.get(0);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> embeddings = new ArrayList<>();
        for (String text : texts) {
            float[] vec = embedSingle(text);
            if (vec.length > 0) embeddings.add(vec);
        }
        return embeddings;
    }

    @SuppressWarnings("unchecked")
    private float[] embedSingle(String text) {
        try {
            Map<String, String> input = new HashMap<>();
            input.put("type", "text");
            input.put("text", text);

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            // encoding_format removed — doubao-embedding-vision returns float by default
            body.put("input", List.of(input));

            String effectiveKey = aiConfig.resolveEmbeddingKey(redis);
            Request request = new Request.Builder()
                    .url(baseUrl + "/embeddings/multimodal")
                    .addHeader("Authorization", "Bearer " + effectiveKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(objectMapper.writeValueAsString(body),
                            MediaType.parse("application/json")))
                    .build();

            Response response = httpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                log.error("Embedding 请求失败: code={}, body={}", response.code(), errorBody);
                return new float[0];
            }

            String responseBody = response.body().string();
            Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);

            Object dataObj = result.get("data");
            Map<String, Object> item;
            if (dataObj instanceof List<?> list && !list.isEmpty()) {
                item = (Map<String, Object>) list.get(0);
            } else if (dataObj instanceof Map<?, ?> single) {
                item = (Map<String, Object>) single;
            } else {
                return new float[0];
            }

            List<Number> embedding = (List<Number>) item.get("embedding");
            float[] vec = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vec[i] = embedding.get(i).floatValue();
            }
            return vec;

        } catch (IOException e) {
            log.error("Embedding 请求异常", e);
            return new float[0];
        }
    }
}
