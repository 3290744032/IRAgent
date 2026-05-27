package com.suiyuan.iragent_app.data.repository.v3;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.suiyuan.iragent_app.data.model.v3.ChatRequestV3;
import com.suiyuan.iragent_app.data.model.v3.NoteRef;
import com.suiyuan.iragent_app.data.remote.NetworkClient;
import com.suiyuan.iragent_app.data.remote.v3.NetworkClientV3;
import com.suiyuan.iragent_app.util.SseParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatRepositoryV3 {

    private static final String TAG = "ChatRepositoryV3";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient streamClient;
    private final String baseUrl;

    public ChatRepositoryV3() {
        this.streamClient = NetworkClientV3.getStreamOkHttpClient();
        this.baseUrl = NetworkClientV3.getBaseUrl();
    }

    public interface ChatStreamCallback {
        void onStart();
        void onChunk(String content);
        void onNoteRefs(List<NoteRef> noteRefs);
        void onDone();
        void onError(int code, String message);
        void onException(Exception e);
    }

    public void chatStreamWithImage(InputStream imageStream, String question, String conversationId, ChatStreamCallback callback) {
        try {
            byte[] imageBytes = readBytes(imageStream);
            String q = (question != null && !question.trim().isEmpty()) ? question : "请帮我解答这道题";
            Log.d(TAG, "chatStreamWithImage: POST " + baseUrl + "chat/stream-image question=" + q + " imageSize=" + imageBytes.length);

            MultipartBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("question", q)
                    .addFormDataPart("image", "image.jpg", RequestBody.create(imageBytes, MediaType.parse("image/jpeg")))
                    .build();

            Request request = new Request.Builder()
                    .url(baseUrl + "chat/stream-image")
                    .header("token", NetworkClient.getToken())
                    .post(body)
                    .build();

            streamClient.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    Log.e(TAG, "chatStreamWithImage onFailure: " + e.getMessage(), e);
                    callback.onException(e);
                }

                @Override
                public void onResponse(okhttp3.Call call, Response response) throws IOException {
                    Log.d(TAG, "chatStreamWithImage response: code=" + response.code());

                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "null";
                        Log.e(TAG, "chatStreamWithImage error: " + response.code() + " " + errorBody);
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
                        parser.setV3Callback(new SseParser.V3Callback() {
                            @Override
                            public void onChunk(String content) {
                                Log.v(TAG, "onChunk: " + content);
                                callback.onChunk(content);
                            }

                            @Override
                            public void onNoteRefs(List<NoteRef> noteRefs) {
                                Log.d(TAG, "onNoteRefs: " + noteRefs.size() + " refs");
                                callback.onNoteRefs(noteRefs);
                            }

                            @Override
                            public void onStep(String step, String text, int current, int total) {}

                            @Override
                            public void onComplete(com.suiyuan.iragent_app.data.model.v3.GradingReport report) {}

                            @Override
                            public void onDone() {
                                Log.d(TAG, "onDone");
                                callback.onDone();
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "parser onError: " + error);
                                callback.onError(-1, error);
                            }
                        });
                        callback.onStart();
                        parser.parse();
                    } catch (Exception e) {
                        Log.e(TAG, "chatStreamWithImage parse exception: " + e.getMessage(), e);
                        callback.onException(e);
                    } finally {
                        response.close();
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "chatStreamWithImage exception: " + e.getMessage(), e);
            callback.onException(e instanceof IOException ? (IOException) e : new IOException(e));
        }
    }

    private byte[] readBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int read;
        while ((read = is.read(buf)) != -1) buffer.write(buf, 0, read);
        return buffer.toByteArray();
    }

    public void chatStream(String question, String conversationId, ChatStreamCallback callback) {
        ChatRequestV3 requestBody = new ChatRequestV3(question, conversationId);
        String json = new Gson().toJson(requestBody);
        Log.d(TAG, "chatStream request: POST " + baseUrl + "chat/stream body=" + json);

        RequestBody body = RequestBody.create(json, JSON);

        Request request = new Request.Builder()
                .url(baseUrl + "chat/stream")
                .header("Accept", "text/event-stream")
                .header("token", NetworkClient.getToken())
                .post(body)
                .build();

        streamClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e(TAG, "chatStream onFailure: " + e.getMessage(), e);
                callback.onException(e);
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                Log.d(TAG, "chatStream response: code=" + response.code() + " message=" + response.message());

                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "null";
                    Log.e(TAG, "chatStream error: " + response.code() + " " + response.message() + " body=" + errorBody);
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
                    parser.setV3Callback(new SseParser.V3Callback() {
                        @Override
                        public void onChunk(String content) {
                            Log.v(TAG, "onChunk: " + content);
                            callback.onChunk(content);
                        }

                        @Override
                        public void onNoteRefs(List<NoteRef> noteRefs) {
                            Log.d(TAG, "onNoteRefs: " + noteRefs.size() + " refs");
                            callback.onNoteRefs(noteRefs);
                        }

                        @Override
                        public void onStep(String step, String text, int current, int total) {
                            // chat流中不处理step事件
                        }

                        @Override
                        public void onComplete(com.suiyuan.iragent_app.data.model.v3.GradingReport report) {
                            // chat流中不处理complete事件
                        }

                        @Override
                        public void onDone() {
                            Log.d(TAG, "onDone");
                            callback.onDone();
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "parser onError: " + error);
                            callback.onError(-1, error);
                        }
                    });
                    callback.onStart();
                    parser.parse();
                } catch (Exception e) {
                    Log.e(TAG, "chatStream parse exception: " + e.getMessage(), e);
                    callback.onException(e);
                } finally {
                    response.close();
                }
            }
        });
    }
}
