package com.suiyuan.iragent_app.data.repository.v3;

import com.suiyuan.iragent_app.data.model.Conversation;
import com.suiyuan.iragent_app.data.model.CreateConversationRequest;
import com.suiyuan.iragent_app.data.model.GenerateTimelineRequest;
import com.suiyuan.iragent_app.data.model.Message;
import com.suiyuan.iragent_app.data.model.TimelineTitleResponse;
import com.suiyuan.iragent_app.data.remote.ApiService;
import com.suiyuan.iragent_app.data.remote.v3.NetworkClientV3;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConversationRepositoryV3 {

    private final ApiService apiService;

    public ConversationRepositoryV3() {
        this.apiService = NetworkClientV3.getConversationApiService();
    }

    public interface ResultCallback<T> {
        void onSuccess(T data);
        void onError(int code, String message);
        void onException(Exception e);
    }

    public void getConversations(int page, int size, ResultCallback<List<Conversation>> callback) {
        apiService.getConversations(page, size).enqueue(new Callback<com.suiyuan.iragent_app.data.model.ApiResponse<List<Conversation>>>() {
            @Override
            public void onResponse(Call<com.suiyuan.iragent_app.data.model.ApiResponse<List<Conversation>>> call,
                                   Response<com.suiyuan.iragent_app.data.model.ApiResponse<List<Conversation>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData() != null ? response.body().getData() : new ArrayList<>());
                } else {
                    callback.onError(response.code(), response.message());
                }
            }

            @Override
            public void onFailure(Call<com.suiyuan.iragent_app.data.model.ApiResponse<List<Conversation>>> call, Throwable t) {
                callback.onException(new Exception(t));
            }
        });
    }

    public void createConversation(String name, String description, ResultCallback<Conversation> callback) {
        CreateConversationRequest request = new CreateConversationRequest(name, description);
        apiService.createConversation(request).enqueue(new Callback<com.suiyuan.iragent_app.data.model.ApiResponse<com.suiyuan.iragent_app.data.model.ConversationListResponse>>() {
            @Override
            public void onResponse(Call<com.suiyuan.iragent_app.data.model.ApiResponse<com.suiyuan.iragent_app.data.model.ConversationListResponse>> call,
                                   Response<com.suiyuan.iragent_app.data.model.ApiResponse<com.suiyuan.iragent_app.data.model.ConversationListResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()
                        && response.body().getData() != null && response.body().getData().getConversation() != null) {
                    callback.onSuccess(response.body().getData().getConversation());
                } else {
                    callback.onError(response.code(), response.message());
                }
            }

            @Override
            public void onFailure(Call<com.suiyuan.iragent_app.data.model.ApiResponse<com.suiyuan.iragent_app.data.model.ConversationListResponse>> call, Throwable t) {
                callback.onException(new Exception(t));
            }
        });
    }

    public void getMessages(String conversationId, ResultCallback<List<Message>> callback) {
        apiService.getConversationMessages(conversationId).enqueue(new Callback<com.suiyuan.iragent_app.data.model.ApiResponse<List<Message>>>() {
            @Override
            public void onResponse(Call<com.suiyuan.iragent_app.data.model.ApiResponse<List<Message>>> call,
                                   Response<com.suiyuan.iragent_app.data.model.ApiResponse<List<Message>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData() != null ? response.body().getData() : new ArrayList<>());
                } else {
                    callback.onError(response.code(), response.message());
                }
            }

            @Override
            public void onFailure(Call<com.suiyuan.iragent_app.data.model.ApiResponse<List<Message>>> call, Throwable t) {
                callback.onException(new Exception(t));
            }
        });
    }

    public void generateTitle(String topic, String conversationId, ResultCallback<TimelineTitleResponse> callback) {
        GenerateTimelineRequest request = new GenerateTimelineRequest(topic, conversationId);
        apiService.generateTimelineTitle(request).enqueue(new Callback<com.suiyuan.iragent_app.data.model.ApiResponse<TimelineTitleResponse>>() {
            @Override
            public void onResponse(Call<com.suiyuan.iragent_app.data.model.ApiResponse<TimelineTitleResponse>> call,
                                   Response<com.suiyuan.iragent_app.data.model.ApiResponse<TimelineTitleResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(response.code(), response.message());
                }
            }

            @Override
            public void onFailure(Call<com.suiyuan.iragent_app.data.model.ApiResponse<TimelineTitleResponse>> call, Throwable t) {
                callback.onException(new Exception(t));
            }
        });
    }
}
