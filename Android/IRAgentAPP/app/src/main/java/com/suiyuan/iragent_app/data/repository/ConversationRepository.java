package com.suiyuan.iragent_app.data.repository;

import com.suiyuan.iragent_app.data.local.ConversationDao;
import com.suiyuan.iragent_app.data.local.ConversationEntity;
import com.suiyuan.iragent_app.data.model.*;
import com.suiyuan.iragent_app.data.remote.ApiService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConversationRepository {

    private final ApiService apiService;
    private final ConversationDao conversationDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ConversationRepository(ApiService apiService, ConversationDao conversationDao) {
        this.apiService = apiService;
        this.conversationDao = conversationDao;
    }

    public interface ResultCallback<T> {
        void onSuccess(T data);
        void onError(int code, String message);
        void onException(Exception e);
    }

    private <T, R> Callback<ApiResponse<T>> createCallback(ResultCallback<R> callback, DataExtractor<T, R> extractor) {
        return new Callback<ApiResponse<T>>() {
            @Override
            public void onResponse(Call<ApiResponse<T>> call, Response<ApiResponse<T>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<T> body = response.body();
                    if (body.isSuccess()) {
                        R data = extractor.extract(body);
                        if (data != null) {
                            callback.onSuccess(data);
                        } else {
                            callback.onError(body.getCode(), body.getMessage());
                        }
                    } else {
                        callback.onError(body.getCode(), body.getMessage());
                    }
                } else {
                    callback.onError(response.code(), response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<T>> call, Throwable t) {
                callback.onException(new Exception(t));
            }
        };
    }

    private interface DataExtractor<T, R> {
        R extract(ApiResponse<T> response);
    }

    public void getConversationsFromServer(int page, int size, ResultCallback<List<Conversation>> callback) {
        apiService.getConversations(page, size).enqueue(createCallback(callback, response -> {
            if (response.getData() == null) return null;
            // API 直接返回 List<Conversation>，无需包装
            return response.getData();
        }));
    }

    public void getAllConversationsFromServer(ResultCallback<List<Conversation>> callback) {
        apiService.getAllConversations().enqueue(new Callback<ApiResponse<List<Conversation>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Conversation>>> call, Response<ApiResponse<List<Conversation>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Conversation> data = response.body().getData();
                    if (data != null) {
                        executor.execute(() -> {
                            List<ConversationEntity> entities = new ArrayList<>();
                            for (Conversation conversation : data) {
                                entities.add(toEntity(conversation));
                            }
                            conversationDao.insertConversations(entities);
                            
                            List<Conversation> result = new ArrayList<>();
                            for (ConversationEntity entity : conversationDao.getAllConversationsSync()) {
                                result.add(toModel(entity));
                            }
                            final List<Conversation> finalResult = result;
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                callback.onSuccess(finalResult);
                            });
                        });
                    } else {
                        final List<Conversation> emptyList = new ArrayList<>();
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            callback.onSuccess(emptyList);
                        });
                    }
                } else {
                    final int errorCode = response.code();
                    final String errorMsg = response.message();
                    android.util.Log.e("ConversationRepo", "getAllConversations API error: code=" + errorCode + ", msg=" + errorMsg);
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onError(errorCode, errorMsg);
                    });
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Conversation>>> call, Throwable t) {
                android.util.Log.e("ConversationRepo", "getAllConversations API failure: " + t.getMessage());
                final Exception e = new Exception(t);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onException(e);
                });
            }
        });
    }

    public void syncConversations(ResultCallback<List<Conversation>> callback) {
        getAllConversationsFromServer(callback);
    }
    
    public void syncConversations() {
        syncConversations(new ResultCallback<List<Conversation>>() {
            @Override
            public void onSuccess(List<Conversation> data) {}
            @Override
            public void onError(int code, String message) {}
            @Override
            public void onException(Exception e) {}
        });
    }

    public void createConversation(String name, String description, ResultCallback<Conversation> callback) {
        CreateConversationRequest request = new CreateConversationRequest(name, description);
        apiService.createConversation(request).enqueue(createCallback(callback, response -> {
            if (response.getData() == null || response.getData().getConversation() == null) return null;
            Conversation conversation = response.getData().getConversation();
            executor.execute(() -> conversationDao.insertConversation(toEntity(conversation)));
            return conversation;
        }));
    }

    public void updateConversation(String id, String name, String description, String status, ResultCallback<Conversation> callback) {
        UpdateConversationRequest request = new UpdateConversationRequest(name, description, status);
        apiService.updateConversation(id, request).enqueue(createCallback(callback, response -> {
            if (response.getData() == null || response.getData().getConversation() == null) return null;
            Conversation conversation = response.getData().getConversation();
            executor.execute(() -> conversationDao.insertConversation(toEntity(conversation)));
            return conversation;
        }));
    }

    public void deleteConversation(String id, ResultCallback<Void> callback) {
        apiService.deleteConversation(id).enqueue(createCallback(callback, response -> {
            executor.execute(() -> conversationDao.deleteConversation(id));
            return null;
        }));
    }

    public void getConversationFromServer(String id, ResultCallback<Conversation> callback) {
        apiService.getConversation(id).enqueue(createCallback(callback, response -> {
            if (response.getData() == null || response.getData().getConversation() == null) return null;
            Conversation conversation = response.getData().getConversation();
            // 同步更新本地缓存
            executor.execute(() -> conversationDao.insertConversation(toEntity(conversation)));
            return conversation;
        }));
    }

    public void getConversationMessagesFromServer(String conversationId, ResultCallback<List<Message>> callback) {
        apiService.getConversationMessages(conversationId).enqueue(createCallback(callback, response -> {
            return response.getData();
        }));
    }

    public void getAllConversations(ResultCallback<List<Conversation>> callback) {
        executor.execute(() -> {
            try {
                List<ConversationEntity> entities = conversationDao.getAllConversationsSync();
                List<Conversation> conversations = new ArrayList<>();
                for (ConversationEntity entity : entities) {
                    conversations.add(toModel(entity));
                }
                List<Conversation> finalResult = conversations;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onSuccess(finalResult);
                });
            } catch (Exception e) {
                final Exception ex = e;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onException(ex);
                });
            }
        });
    }

    private Conversation toModel(ConversationEntity entity) {
        Conversation conversation = new Conversation();
        conversation.setConversationId(entity.getId());
        conversation.setUserId(entity.getUserId());
        conversation.setName(entity.getName());
        conversation.setDescription(entity.getDescription());
        conversation.setCreatedAt(entity.getCreatedAt());
        conversation.setUpdatedAt(entity.getUpdatedAt());
        conversation.setStatus(entity.getStatus());
        return conversation;
    }

    public void togglePin(String id, boolean isPinned) {
        conversationDao.updatePinnedStatus(id, isPinned);
    }

    private ConversationEntity toEntity(Conversation conversation) {
        return new ConversationEntity(
                conversation.getConversationId(),
                conversation.getUserId(),
                conversation.getName(),
                conversation.getDescription(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                conversation.getStatus(),
                false,
                0
        );
    }
}