package com.suiyuan.iragent_app.data.repository.v3;

import android.util.Log;

import com.google.gson.Gson;
import com.suiyuan.iragent_app.data.model.v3.SmartPaper;
import com.suiyuan.iragent_app.data.remote.NetworkClient;
import com.suiyuan.iragent_app.data.remote.v3.NetworkClientV3;
import com.suiyuan.iragent_app.util.SseParser;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SmartPaperStreamRepository {

    private static final String TAG = "SmartPaperStreamRepo";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient streamClient;
    private final String baseUrl;
    private final Gson gson;

    public SmartPaperStreamRepository() {
        this.streamClient = NetworkClientV3.getStreamOkHttpClient();
        this.baseUrl = NetworkClientV3.getBaseUrl();
        this.gson = new Gson();
    }

    public interface StreamCallback {
        void onChunk(String content);
        void onComplete(String paperId, SmartPaper paper);
        void onError(int code, String message);
        void onException(Exception e);
    }

    public void streamGeneratePaper(String prompt, StreamCallback callback) {
        Map<String, String> bodyMap = new HashMap<>();
        bodyMap.put("prompt", prompt);
        String json = gson.toJson(bodyMap);
        Log.d(TAG, "streamGeneratePaper: POST " + baseUrl + "paper/smart/stream body=" + json);

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "paper/smart/stream")
                .post(body)
                .build();

        streamClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e(TAG, "onFailure: " + e.getMessage(), e);
                callback.onException(e);
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                Log.d(TAG, "onResponse: code=" + response.code());

                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "null";
                    Log.e(TAG, "error: " + response.code() + " " + errorBody);
                    callback.onError(response.code(), response.message() + ": " + errorBody);
                    response.close();
                    return;
                }

                if (response.body() == null) {
                    callback.onError(-1, "响应体为空");
                    response.close();
                    return;
                }

                try {
                    InputStream is = response.body().byteStream();
                    SseParser parser = new SseParser(is);

                    AtomicBoolean handled = new AtomicBoolean(false);

                    parser.parseWithHandler(new SseParser.EventHandler() {
                        @Override
                        public void onEvent(String type, JSONObject payload) {
                            Log.v(TAG, "SSE event: type=" + type + " payload=" +
                                    (payload != null ? payload.toString() : "null"));

                            if ("chunk".equals(type) && payload != null) {
                                String content = payload.optString("content", "");
                                if (!content.isEmpty()) {
                                    callback.onChunk(content);
                                }
                            } else if ("complete".equals(type) && payload != null) {
                                handled.set(true);
                                String paperId = payload.optString("paperId", "");
                                SmartPaper paper = parseSmartPaper(payload);
                                Log.d(TAG, "stream complete: paperId=" + paperId + " questions=" +
                                        (paper != null ? paper.getQuestionCount() : 0));
                                callback.onComplete(paperId, paper);
                            } else if ("error".equals(type)) {
                                handled.set(true);
                                String message = payload != null
                                        ? payload.optString("message", "未知错误")
                                        : "未知错误";
                                Log.e(TAG, "stream error: " + message);
                                callback.onError(-1, message);
                            } else if ("done".equals(type)) {
                                if (!handled.get()) {
                                    Log.w(TAG, "stream ended without complete event");
                                }
                            }
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "parser onError: " + error);
                            callback.onError(-1, error);
                        }
                    });

                } catch (Exception e) {
                    Log.e(TAG, "parse exception: " + e.getMessage(), e);
                    callback.onException(e);
                } finally {
                    response.close();
                }
            }
        });
    }

    private SmartPaper parseSmartPaper(JSONObject payload) {
        try {
            String json = payload.toString();
            return gson.fromJson(json, SmartPaper.class);
        } catch (Exception e) {
            Log.w(TAG, "parseSmartPaper failed: " + e.getMessage());
            return null;
        }
    }
}
