package com.suiyuan.iragent_app.util;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TtsHttpClient {

    private static final String TAG = "TtsHttpClient";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public static final String API_URL = "https://openspeech.bytedance.com/api/v1/tts";
    public static final String API_KEY = "d195a888-8a60-4dcd-890d-07ad744d2f74";
    public static final int DEFAULT_TIMEOUT_MS = 5000;

    private static final Gson gson = new Gson();
    private static volatile OkHttpClient client;

    private static OkHttpClient getClient(int timeoutMs) {
        if (client == null) {
            synchronized (TtsHttpClient.class) {
                if (client == null) {
                    client = new OkHttpClient.Builder()
                            .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                            .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                            .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                            .build();
                }
            }
        }
        return client;
    }

    /**
     * 清洗数学公式文本，让 TTS 能正确朗读
     * 规则：去掉 $$/$ 分隔符、替换 Unicode 数学符号为口语化表述
     */
    public static String cleanForTts(String text) {
        if (text == null || text.isEmpty()) return text;

        String s = text;

        // 1. 去掉 $$ 显示公式定界符，保留内部文本
        s = s.replaceAll("\\$\\$([\\s\\S]*?)\\$\\$", "$1");

        // 2. 去掉 $ 行内公式定界符，保留内部文本（使用 [\\s\\S] 替代 [^$] 以匹配换行）
        s = s.replaceAll("\\$([\\s\\S]*?)\\$", "$1");

        // 3. 替换 Unicode 上标/符号为口语化表述
        s = s.replace("\u00b2", "\u7684\u5e73\u65b9");   // ² → 的平方
        s = s.replace("\u00b3", "\u7684\u7acb\u65b9");   // ³ → 的立方
        s = s.replace("\u221a", "\u6839\u53f7");          // √ → 根号
        s = s.replace("\u03c0", "\u03c0");                // π 保留（TTS 支持）
        s = s.replace("\u2248", "\u7ea6\u7b49\u4e8e");    // ≈ → 约等于
        s = s.replace("\u2260", "\u4e0d\u7b49\u4e8e");    // ≠ → 不等于
        s = s.replace("\u2264", "\u5c0f\u4e8e\u7b49\u4e8e"); // ≤ → 小于等于
        s = s.replace("\u2265", "\u5927\u4e8e\u7b49\u4e8e"); // ≥ → 大于等于

        return s.trim();
    }

    /**
     * 请求火山引擎 TTS，返回 Base64 编码的音频数据
     *
     * @param text      要合成的文本
     * @param timeoutMs 超时时间（毫秒）
     * @return Base64 音频字符串，失败返回 null
     */
    public static String requestTts(String text, int timeoutMs) {
        try {
            TtsRequest ttsRequest = new TtsRequest(text);
            String jsonBody = gson.toJson(ttsRequest);

            Log.d(TAG, "Requesting TTS: text=\"" + text + "\" (len=" + text.length() + ")");

            Request request = new Request.Builder()
                    .url(API_URL)
                    .header("x-api-key", API_KEY)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, JSON))
                    .build();

            try (Response response = getClient(timeoutMs).newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.w(TAG, "TTS API error: " + response.code());
                    return null;
                }

                String responseBody = response.body().string();
                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

                if (json.has("data")) {
                    return json.get("data").getAsString();
                } else if (json.has("code") && json.get("code").getAsInt() != 0) {
                    String msg = json.has("message") ? json.get("message").getAsString() : "unknown";
                    Log.w(TAG, "TTS API returned error code=" + json.get("code") + " msg=" + msg);
                    return null;
                } else {
                    Log.w(TAG, "TTS API response missing data field");
                    return null;
                }
            }
        } catch (IOException e) {
            Log.w(TAG, "TTS network request failed", e);
            return null;
        } catch (Exception e) {
            Log.w(TAG, "TTS unexpected error", e);
            return null;
        }
    }

    /**
     * 使用默认超时
     */
    public static String requestTts(String text) {
        return requestTts(text, DEFAULT_TIMEOUT_MS);
    }
}
