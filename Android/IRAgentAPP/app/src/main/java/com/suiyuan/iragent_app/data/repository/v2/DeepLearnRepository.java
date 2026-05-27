package com.suiyuan.iragent_app.data.repository.v2;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.suiyuan.iragent_app.data.model.ApiResponse;
import com.suiyuan.iragent_app.data.model.GenerateTimelineRequest;
import com.suiyuan.iragent_app.data.model.v2.*;
import com.suiyuan.iragent_app.data.remote.v2.ApiServiceV2;
import com.suiyuan.iragent_app.data.remote.v2.NetworkClientV2;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeepLearnRepository {

    private static final String TAG = "DeepLearnRepository";
    private static final boolean DEBUG = true;

    private final ApiServiceV2 apiService;
    private final ApiServiceV2 streamApiService;

    public DeepLearnRepository() {
        this.apiService = NetworkClientV2.getApiService();
        this.streamApiService = NetworkClientV2.getStreamApiService();
    }

    public DeepLearnRepository(ApiServiceV2 apiService, ApiServiceV2 streamApiService) {
        this.apiService = apiService;
        this.streamApiService = streamApiService;
    }

    public interface ResultCallback<T> {
        void onSuccess(T data);
        void onError(int code, String message);
        void onException(Exception e);
    }

    public interface SummaryCallback {
        void onStart(String message);
        void onContent(String text);
        void onSummaryData(SessionSummaryResponse data);
        void onError(int code, String message);
        void onException(Exception e);
    }

    public interface TeachCallback {
        void onStart(String message);
        void onContent(String text);
        void onTimeline(String timelineJson);
        void onDone();
        void onCompleted();
        void onError(int code, String message);
        void onException(Exception e);
    }

    private static class MainThreadTeachCallback implements TeachCallback {
        private final TeachCallback callback;
        private final Handler handler = new Handler(Looper.getMainLooper());

        MainThreadTeachCallback(TeachCallback callback) { this.callback = callback; }

        @Override public void onStart(String message) { handler.post(() -> callback.onStart(message)); }
        @Override public void onContent(String text) { handler.post(() -> callback.onContent(text)); }
        @Override public void onTimeline(String timelineJson) { handler.post(() -> callback.onTimeline(timelineJson)); }
        @Override public void onDone() { handler.post(callback::onDone); }
        @Override public void onCompleted() { handler.post(callback::onCompleted); }
        @Override public void onError(int code, String message) { handler.post(() -> callback.onError(code, message)); }
        @Override public void onException(Exception e) { handler.post(() -> callback.onException(e)); }
    }

    private static class MainThreadSummaryCallback implements SummaryCallback {
        private final SummaryCallback callback;
        private final Handler handler = new Handler(Looper.getMainLooper());

        MainThreadSummaryCallback(SummaryCallback callback) { this.callback = callback; }

        @Override public void onStart(String message) { handler.post(() -> callback.onStart(message)); }
        @Override public void onContent(String text) { handler.post(() -> callback.onContent(text)); }
        @Override public void onSummaryData(SessionSummaryResponse data) { handler.post(() -> callback.onSummaryData(data)); }
        @Override public void onError(int code, String message) { handler.post(() -> callback.onError(code, message)); }
        @Override public void onException(Exception e) { handler.post(() -> callback.onException(e)); }
    }

    public void createSession(String question, String subjectType, ResultCallback<SessionResponse> callback) {
        apiService.createSession(new CreateSessionRequest(question, subjectType))
                .enqueue(new Callback<ApiResponse<SessionResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<SessionResponse>> call, Response<ApiResponse<SessionResponse>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            callback.onSuccess(response.body().getData());
                        } else {
                            String msg = response.body() != null ? response.body().getMessage() : "创建失败";
                            callback.onError(response.code(), msg);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<SessionResponse>> call, Throwable t) {
                        callback.onException(new Exception(t));
                    }
                });
    }

    public void getSessionDetail(String sessionId, ResultCallback<SessionResponse> callback) {
        apiService.getSessionDetail(sessionId)
                .enqueue(new Callback<ApiResponse<SessionResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<SessionResponse>> call, Response<ApiResponse<SessionResponse>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            callback.onSuccess(response.body().getData());
                        } else {
                            String msg = response.body() != null ? response.body().getMessage() : "获取失败";
                            callback.onError(response.code(), msg);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<SessionResponse>> call, Throwable t) {
                        callback.onException(new Exception(t));
                    }
                });
    }

    public void submitAnswer(String sessionId, String answer, ResultCallback<AnswerResponse> callback) {
        if (DEBUG) Log.d(TAG, "submitAnswer: sessionId=" + sessionId + ", answer=" + answer);
        apiService.submitAnswer(sessionId, new AnswerRequest(answer))
                .enqueue(new Callback<ApiResponse<AnswerResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<AnswerResponse>> call, Response<ApiResponse<AnswerResponse>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            if (DEBUG) Log.d(TAG, "submitAnswer success");
                            callback.onSuccess(response.body().getData());
                        } else {
                            String msg = response.body() != null ? response.body().getMessage() : "提交失败";
                            if (DEBUG) Log.e(TAG, "submitAnswer error: " + msg);
                            callback.onError(response.code(), msg);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<AnswerResponse>> call, Throwable t) {
                        if (DEBUG) Log.e(TAG, "submitAnswer onFailure", t);
                        callback.onException(new Exception(t));
                    }
                });
    }

    public void summaryStream(String sessionId, SummaryCallback callback) {
        if (DEBUG) Log.d(TAG, "summaryStream called: sessionId=" + sessionId);

        MainThreadSummaryCallback mainCb = new MainThreadSummaryCallback(callback);
        streamApiService.getSummaryStream(sessionId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (DEBUG) Log.d(TAG, "summaryStream response: code=" + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    InputStream stream = response.body().byteStream();
                    new Thread(() -> {
                        parseSummaryStream(stream, mainCb);
                    }).start();
                } else {
                    mainCb.onError(response.code(), "获取总结失败, code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (DEBUG) Log.e(TAG, "summaryStream onFailure", t);
                mainCb.onException(new Exception(t));
            }
        });
    }

    private void parseSummaryStream(InputStream inputStream, SummaryCallback callback) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            StringBuilder dataBuilder = new StringBuilder();
            boolean summaryReceived = false;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data:")) {
                    dataBuilder.append(line.substring(5).trim());
                } else if (line.isEmpty() && dataBuilder.length() > 0) {
                    String data = dataBuilder.toString().trim();
                    if (DEBUG) Log.d(TAG, "summary SSE data: " + data);
                    if (parseSummaryData(data, callback)) summaryReceived = true;
                    dataBuilder.setLength(0);
                }
            }

            if (dataBuilder.length() > 0) {
                if (parseSummaryData(dataBuilder.toString().trim(), callback)) summaryReceived = true;
            }

            if (!summaryReceived) {
                if (DEBUG) Log.w(TAG, "summaryStream ended without summary data");
                callback.onError(500, "未获取到学习总结数据");
            }

            Log.d(TAG, "summaryStream ended normally");
        } catch (Exception e) {
            if (DEBUG) Log.e(TAG, "parseSummaryStream exception", e);
            callback.onException(e);
        }
    }

    private boolean parseSummaryData(String jsonStr, SummaryCallback callback) {
        try {
            String cleanJson = jsonStr.trim();
            if (cleanJson.startsWith("\"") && cleanJson.endsWith("\"")) {
                cleanJson = cleanJson.substring(1, cleanJson.length() - 1)
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\");
            }

            JSONObject json = new JSONObject(cleanJson);
            String type = json.optString("type");

            switch (type) {
                case "start":
                    callback.onStart(json.optString("message", ""));
                    return false;
                case "text":
                    callback.onContent(json.optString("content", ""));
                    return false;
                case "summary":
                    Gson gson = new Gson();
                    SessionSummaryResponse summary = gson.fromJson(json.getJSONObject("data").toString(), SessionSummaryResponse.class);
                    callback.onSummaryData(summary);
                    return true;
                case "error":
                    callback.onError(json.optInt("code", 500), json.optString("message", "未知错误"));
                    return true;
                default:
                    if (DEBUG) Log.w(TAG, "Unknown summary type: " + type);
                    return false;
            }
        } catch (Exception e) {
            if (DEBUG) Log.e(TAG, "parseSummaryData error", e);
            callback.onException(e);
            return true;
        }
    }

    public void getSessionHistory(int page, int size, ResultCallback<SessionHistoryResponse> callback) {
        apiService.getSessionHistory(page, size)
                .enqueue(new Callback<ApiResponse<SessionHistoryResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<SessionHistoryResponse>> call, Response<ApiResponse<SessionHistoryResponse>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            callback.onSuccess(response.body().getData());
                        } else {
                            String msg = response.body() != null ? response.body().getMessage() : "获取历史失败";
                            callback.onError(response.code(), msg);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<SessionHistoryResponse>> call, Throwable t) {
                        callback.onException(new Exception(t));
                    }
                });
    }

    public void deleteSession(String sessionId, ResultCallback<DeleteSessionResponse> callback) {
        apiService.deleteSession(sessionId)
                .enqueue(new Callback<ApiResponse<DeleteSessionResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<DeleteSessionResponse>> call, Response<ApiResponse<DeleteSessionResponse>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            callback.onSuccess(response.body().getData());
                        } else {
                            String msg = response.body() != null ? response.body().getMessage() : "删除失败";
                            callback.onError(response.code(), msg);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<DeleteSessionResponse>> call, Throwable t) {
                        callback.onException(new Exception(t));
                    }
                });
    }

    public void generateTimelineSync(String topic, ResultCallback<String> callback) {
        if (DEBUG) Log.d(TAG, "generateTimelineSync: topic=" + topic);

        apiService.generateTimeline(new GenerateTimelineRequest(topic)).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (DEBUG) Log.d(TAG, "generateTimelineSync response: code=" + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String rawJson = response.body().string();
                        if (DEBUG) Log.d(TAG, "generateTimelineSync raw: " + rawJson);

                        JSONObject root = new JSONObject(rawJson);
                        if (root.optBoolean("success", false) && root.has("data")) {
                            String cleanJson = normalizeTimelineJson(root.getJSONObject("data").toString());
                            callback.onSuccess(cleanJson);
                        } else {
                            String msg = root.optString("message", "生成失败");
                            callback.onError(response.code(), msg);
                        }
                    } catch (Exception e) {
                        if (DEBUG) Log.e(TAG, "generateTimelineSync parse error", e);
                        callback.onException(e);
                    }
                } else {
                    callback.onError(response.code(), "同步生成失败, code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (DEBUG) Log.e(TAG, "generateTimelineSync onFailure", t);
                callback.onException(new Exception(t));
            }
        });
    }

    private String normalizeTimelineJson(String timelineJson) {
        try {
            JSONObject obj = new JSONObject(timelineJson);
            if (obj.has("viewBox")) {
                JSONObject vb = obj.getJSONObject("viewBox");
                double xRange = vb.optDouble("xRange", 0);
                double yRange = vb.optDouble("yRange", 0);
                if (xRange == 0 && vb.has("xMin") && vb.has("xMax")) {
                    xRange = Math.max(Math.abs(vb.getDouble("xMin")), Math.abs(vb.getDouble("xMax")));
                    vb.put("xRange", xRange);
                }
                if (yRange == 0 && vb.has("yMin") && vb.has("yMax")) {
                    yRange = Math.max(Math.abs(vb.getDouble("yMin")), Math.abs(vb.getDouble("yMax")));
                    vb.put("yRange", yRange);
                }
            }
            return obj.toString();
        } catch (JSONException e) {
            return timelineJson;
        }
    }

    public void teachStream(String sessionId, String mode, TeachCallback callback) {
        if (DEBUG) Log.d(TAG, "teachStream called: sessionId=" + sessionId + ", mode=" + mode);

        MainThreadTeachCallback mainCb = new MainThreadTeachCallback(callback);
        streamApiService.getTeachStream(sessionId, mode).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (DEBUG) Log.d(TAG, "teachStream response: code=" + response.code() + ", isSuccessful=" + response.isSuccessful());

                if (response.isSuccessful() && response.body() != null) {
                    InputStream stream = response.body().byteStream();
                    new Thread(() -> {
                        parseTeachStream(stream, mainCb);
                        Log.d(TAG, "teachStream: SSE parsing thread finished for sessionId=" + sessionId);
                    }).start();
                } else {
                    String errorMsg = "获取讲解失败, code: " + response.code();
                    if (DEBUG) Log.e(TAG, errorMsg);
                    mainCb.onError(response.code(), errorMsg);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (DEBUG) Log.e(TAG, "teachStream onFailure", t);
                mainCb.onException(new Exception(t));
            }
        });
    }

    private void parseTeachStream(InputStream inputStream, TeachCallback callback) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            StringBuilder dataBuilder = new StringBuilder();
            String currentEvent = null;
            int eventCount = 0;

            while ((line = reader.readLine()) != null) {
                if (DEBUG) Log.v(TAG, "SSE line: [" + line + "]");

                if (line.startsWith("event:")) {
                    currentEvent = line.substring(6).trim();
                } else if (line.isEmpty()) {
                    if (dataBuilder.length() > 0) {
                        String data = dataBuilder.toString().trim();
                        if (DEBUG) Log.d(TAG, "SSE data block: " + data);
                        parseTeachData(data, currentEvent, callback);
                        eventCount++;
                        dataBuilder.setLength(0);
                        currentEvent = null;
                    }
                } else if (line.startsWith("data:")) {
                    dataBuilder.append(line.substring(5).trim());
                }
            }

            if (dataBuilder.length() > 0) {
                String data = dataBuilder.toString().trim();
                if (DEBUG) Log.d(TAG, "SSE data block (EOF): " + data);
                parseTeachData(data, currentEvent, callback);
                eventCount++;
            }

            Log.d(TAG, "SSE stream ended normally, total events parsed: " + eventCount);
        } catch (Exception e) {
            if (DEBUG) Log.e(TAG, "parseTeachStream exception", e);
            callback.onException(e);
        }
    }

    private void parseTeachData(String jsonStr, String eventType, TeachCallback callback) {
        try {
            if (DEBUG) Log.d(TAG, "parseTeachData: eventType=" + eventType + ", json=" + jsonStr);

            String cleanJson = jsonStr.trim();
            if (cleanJson.startsWith("\"") && cleanJson.endsWith("\"")) {
                cleanJson = cleanJson.substring(1, cleanJson.length() - 1)
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\");
            }

            JSONObject json = new JSONObject(cleanJson);
            String type = json.optString("type");

            switch (type) {
                case "start":
                    callback.onStart(json.optString("message", ""));
                    break;
                case "text":
                    callback.onContent(json.optString("content", ""));
                    break;
                case "timeline":
                    callback.onTimeline(json.optString("content", ""));
                    break;
                case "done":
                    if (DEBUG) Log.d(TAG, "parseTeachData: done event received, calling onDone");
                    callback.onDone();
                    break;
                case "completed":
                    if (DEBUG) Log.d(TAG, "parseTeachData: completed event received");
                    callback.onCompleted();
                    break;
                case "error":
                    callback.onError(json.optInt("code", 500), json.optString("message", "未知错误"));
                    break;
                default:
                    if (DEBUG) Log.w(TAG, "Unknown teach type: " + type);
            }
        } catch (JSONException e) {
            if (DEBUG) Log.e(TAG, "parseTeachData JSONException: " + jsonStr, e);
            callback.onException(e);
        }
    }
}
