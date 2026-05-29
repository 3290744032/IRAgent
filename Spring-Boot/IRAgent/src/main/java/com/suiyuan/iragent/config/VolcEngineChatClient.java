package com.suiyuan.iragent.config;

import com.volcengine.ark.runtime.model.responses.content.InputContentItemText;
import com.volcengine.ark.runtime.model.responses.constant.ResponsesConstants;
import com.volcengine.ark.runtime.model.responses.item.ItemEasyMessage;
import com.volcengine.ark.runtime.model.responses.item.MessageContent;
import com.volcengine.ark.runtime.model.responses.request.CreateResponsesRequest;
import com.volcengine.ark.runtime.model.responses.request.ResponsesInput;
import com.volcengine.ark.runtime.model.responses.response.ResponseObject;
import com.volcengine.ark.runtime.service.ArkService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class VolcEngineChatClient {

    private final ArkService arkService;
    private final String model;
    private final String apiKey;

    public VolcEngineChatClient(String apiKey, String model, String baseUrl) {
        this.apiKey = apiKey;
        this.model = (model != null && !model.isEmpty()) ? model : "doubao-seed-1-8-251228";

        String resolvedBaseUrl = (baseUrl != null && !baseUrl.isEmpty()) ? baseUrl : "https://ark.cn-beijing.volces.com/api/v3";

        this.arkService = ArkService.builder()
                .apiKey(apiKey)
                .baseUrl(resolvedBaseUrl)
                .build();

        log.info("火山方舟ChatClient初始化完成: model={}, baseUrl={}", this.model, resolvedBaseUrl);
    }

    public VolcEngineChatClient(String apiKey, String model) {
        this(apiKey, model, null);
    }

    public String chat(String message) {
        try {
            log.debug("发送请求到火山方舟模型: model={}, message={}", model, message);

            CreateResponsesRequest request = CreateResponsesRequest.builder()
                    .model(model)
                    .input(ResponsesInput.builder()
                            .addListItem(ItemEasyMessage.builder()
                                    .role(ResponsesConstants.MESSAGE_ROLE_USER)
                                    .content(MessageContent.builder()
                                            .addListItem(InputContentItemText.builder().text(message).build())
                                            .build())
                                    .build())
                            .build())
                    .build();

            ResponseObject response = arkService.createResponse(request);

            if (response != null) {
                String result = extractResponseContent(response);
                log.debug("收到火山方舟模型回复，长度={}", result != null ? result.length() : 0);
                return result != null ? result : "抱歉，AI服务暂时不可用，请稍后重试。";
            } else {
                log.warn("火山方舟模型返回空响应");
                return "抱歉，AI服务暂时不可用，请稍后重试。";
            }
        } catch (Exception e) {
            log.error("调用火山方舟模型失败: {}", e.getMessage(), e);
            return "抱歉，AI服务暂时不可用，请稍后重试。";
        }
    }

    public String[] streamChat(String message) {
        String response = chat(message);
        if (response == null || response.isEmpty()) {
            return new String[]{"抱歉，AI服务暂时不可用，请稍后重试。"};
        }
        return response.split("\\n");
    }

    @SuppressWarnings("unchecked")
    private String extractResponseContent(ResponseObject response) {
        if (response == null) return null;
        try {
            // 用 Jackson 序列化 SDK 对象为 JSON，再按已知结构解析
            var json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(response);
            var root = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);

            Object output = root.get("output");
            if (output == null) return null;

            StringBuilder result = new StringBuilder();
            if (output instanceof List list) {
                for (Object item : list) {
                    if (item instanceof Map m) {
                        Object content = m.get("content");
                        if (content instanceof List contentList) {
                            for (Object ci : contentList) {
                                if (ci instanceof Map cim) {
                                    Object text = cim.getOrDefault("text", cim.get("value"));
                                    if (text != null) result.append(text);
                                }
                            }
                        } else if (content instanceof String s) {
                            result.append(s);
                        }
                    }
                }
            }
            String finalResult = result.toString();
            return finalResult.isEmpty() ? null : finalResult;
        } catch (Exception e) {
            log.error("提取响应内容失败: {}", e.getMessage(), e);
            return response.toString();
        }
    }

    public void shutdown() {
        if (arkService != null) {
            try {
                arkService.shutdownExecutor();
                log.info("火山方舟ChatClient已关闭");
            } catch (Exception e) {
                log.warn("关闭ArkService时发生异常: {}", e.getMessage());
            }
        }
    }
}
