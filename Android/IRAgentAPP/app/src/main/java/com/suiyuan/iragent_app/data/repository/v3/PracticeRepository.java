package com.suiyuan.iragent_app.data.repository.v3;

import com.google.gson.Gson;
import com.suiyuan.iragent_app.data.model.v3.GradingReport;
import com.suiyuan.iragent_app.data.model.v3.GradingRequest;
import com.suiyuan.iragent_app.data.remote.NetworkClient;
import com.suiyuan.iragent_app.data.remote.v3.NetworkClientV3;
import com.suiyuan.iragent_app.util.SseParser;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PracticeRepository {

    private static final String TAG = "PracticeRepository";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient streamClient;
    private final String baseUrl;

    public PracticeRepository() {
        this.streamClient = NetworkClientV3.getStreamOkHttpClient();
        this.baseUrl = NetworkClientV3.getBaseUrl();
    }

    public interface GradingCallback {
        void onStart();
        void onStep(String step, String text, int current, int total);
        void onComplete(GradingReport report);
        void onError(int code, String message);
        void onException(Exception e);
    }

    public void submitGrading(String content, String subjectType, int maxScore,
                              GradingCallback callback) {
        GradingRequest gradingRequest = new GradingRequest(content, subjectType, maxScore);
        String json = new Gson().toJson(gradingRequest);
        RequestBody body = RequestBody.create(json, JSON);

        Request request = new Request.Builder()
                .url(baseUrl + "grading/submit")
                .header("Accept", "text/event-stream")
                .header("token", NetworkClient.getToken())
                .post(body)
                .build();

        streamClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                callback.onException(e);
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError(response.code(), response.message());
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
                    parser.setV3Callback(new SseParser.V3Callback() {
                        @Override
                        public void onChunk(String content) {
                            // grading流中不处理chunk事件
                        }

                        @Override
                        public void onNoteRefs(java.util.List<com.suiyuan.iragent_app.data.model.v3.NoteRef> noteRefs) {
                            // grading流中不处理note_refs事件
                        }

                        @Override
                        public void onStep(String step, String text, int current, int total) {
                            callback.onStep(step, text, current, total);
                        }

                        @Override
                        public void onComplete(GradingReport report) {
                            callback.onComplete(report);
                        }

                        @Override
                        public void onDone() {
                            // grading流中不处理done事件
                        }

                        @Override
                        public void onError(String error) {
                            callback.onError(-1, error);
                        }
                    });
                    callback.onStart();
                    parser.parse();
                } catch (Exception e) {
                    callback.onException(e);
                } finally {
                    response.close();
                }
            }
        });
    }
}
