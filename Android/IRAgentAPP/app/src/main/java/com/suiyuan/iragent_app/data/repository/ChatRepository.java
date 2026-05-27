package com.suiyuan.iragent_app.data.repository;

import com.suiyuan.iragent_app.data.local.MessageDao;
import com.suiyuan.iragent_app.data.local.MessageEntity;
import com.suiyuan.iragent_app.data.model.ApiResponse;
import com.suiyuan.iragent_app.data.model.GenerateTimelineRequest;
import com.suiyuan.iragent_app.data.model.Message;
import com.suiyuan.iragent_app.data.model.MessageListResponse;
import com.suiyuan.iragent_app.data.model.SolveRequest;
import com.suiyuan.iragent_app.data.model.TimelineTitleResponse;
import com.suiyuan.iragent_app.data.remote.ApiService;
import com.suiyuan.iragent_app.data.remote.NetworkClient;
import com.suiyuan.iragent_app.util.SseParser;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatRepository {

    private final ApiService apiService;
    private final MessageDao messageDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ChatRepository(ApiService apiService, MessageDao messageDao) {
        this.apiService = apiService;
        this.messageDao = messageDao;
    }

    public interface ResultCallback<T> {
        void onSuccess(T data);
        void onError(int code, String message);
        void onException(Exception e);
    }

    public interface StreamCallback {
        void onStart();
        void onText(String text);
        void onGeogebra(String expression);
        void onPlot3D(String config);
        void onTimeline(String json);
        void onDone();
        void onError(int code, String message);
        void onException(Exception e);
    }

    public void solveStream(String problem, String conversationId, StreamCallback callback) {
        SolveRequest request = new SolveRequest(problem, conversationId);
        NetworkClient.getStreamApiService().solveStream(request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    executor.execute(() -> {
                        try {
                            InputStream inputStream = response.body().byteStream();
                            SseParser parser = new SseParser(inputStream);
                            parser.setCallback(new SseParser.Callback() {
                                @Override
                                public void onMessage(String type, String content, String expression) {
                                    switch (type) {
                                        case "start":
                                            callback.onStart();
                                            break;
                                        case "text":
                                            if (content != null) {
                                                callback.onText(content);
                                            }
                                            break;
                                        case "geogebra":
                                        case "plot":
                                            if (expression != null) {
                                                callback.onGeogebra(expression);
                                            }
                                            break;
                                        case "plot3d":
                                            if (content != null) {
                                                callback.onPlot3D(content);
                                            }
                                            break;
                                        case "timeline":
                                            if (content != null) {
                                                callback.onTimeline(content);
                                            }
                                            break;
                                        case "done":
                                            callback.onDone();
                                            break;
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                    callback.onError(500, error);
                                }
                            });
                            parser.parse();
                        } catch (Exception e) {
                            callback.onException(e);
                        }
                    });
                } else {
                    callback.onError(response.code(), response.message());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callback.onException(new Exception(t));
            }
        });
    }

    public void generateTimelineTitle(String topic, String conversationId, ResultCallback<TimelineTitleResponse> callback) {
        GenerateTimelineRequest request = new GenerateTimelineRequest(topic, conversationId);
        apiService.generateTimelineTitle(request).enqueue(new Callback<ApiResponse<TimelineTitleResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TimelineTitleResponse>> call, Response<ApiResponse<TimelineTitleResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(response.code(), response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<TimelineTitleResponse>> call, Throwable t) {
                callback.onException(new Exception(t));
            }
        });
    }

    public void saveMessage(MessageEntity message) {
        if (messageDao != null) {
            executor.execute(() -> messageDao.insertMessage(message));
        }
    }

    public void getMessagesFromServer(String conversationId, ResultCallback<List<Message>> callback) {
        apiService.getChatMessages(conversationId).enqueue(new Callback<ApiResponse<MessageListResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<MessageListResponse>> call, Response<ApiResponse<MessageListResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Message> messages = response.body().getData() != null ? response.body().getData().getMessages() : null;
                    if (messages != null) {
                        callback.onSuccess(messages);
                    } else {
                        callback.onError(response.body().getCode(), response.body().getMessage());
                    }
                } else {
                    callback.onError(response.code(), response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MessageListResponse>> call, Throwable t) {
                callback.onException(new Exception(t));
            }
        });
    }
}