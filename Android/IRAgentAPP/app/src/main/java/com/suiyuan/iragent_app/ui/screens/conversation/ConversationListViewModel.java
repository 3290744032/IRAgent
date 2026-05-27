package com.suiyuan.iragent_app.ui.screens.conversation;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.suiyuan.iragent_app.data.local.ConversationDao;
import com.suiyuan.iragent_app.data.local.ConversationEntity;
import com.suiyuan.iragent_app.data.local.Database;
import com.suiyuan.iragent_app.data.model.Conversation;
import com.suiyuan.iragent_app.data.remote.ApiService;
import com.suiyuan.iragent_app.data.remote.NetworkClient;
import com.suiyuan.iragent_app.data.repository.ConversationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConversationListViewModel extends AndroidViewModel {

    private static final String TAG = "ConversationListVM";
    private final ConversationRepository conversationRepository;
    private final ConversationDao conversationDao;
    private final MutableLiveData<List<ConversationEntity>> conversationsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ConversationEntity>> allConversations = new MutableLiveData<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ConversationListViewModel(@NonNull Application application) {
        super(application);
        ApiService apiService = NetworkClient.getApiService();
        conversationDao = Database.getInstance(application).conversationDao();
        conversationRepository = new ConversationRepository(apiService, conversationDao);
    }

    public LiveData<List<ConversationEntity>> getConversationsLiveData() {
        return conversationsLiveData;
    }

    public void loadConversations() {
        conversationRepository.syncConversations(new ConversationRepository.ResultCallback<List<Conversation>>() {
            @Override
            public void onSuccess(List<Conversation> data) {
                Log.d(TAG, "syncConversations success, data size: " + (data != null ? data.size() : 0));
                executor.execute(() -> {
                    try {
                        List<ConversationEntity> entities = new ArrayList<>();
                        for (Conversation c : data) {
                            entities.add(convertToEntity(c));
                        }
                        allConversations.postValue(entities);
                        conversationsLiveData.postValue(entities);
                    } catch (Exception e) {
                        Log.e(TAG, "Error converting conversations", e);
                    }
                });
            }

            @Override
            public void onError(int code, String message) {
                Log.e(TAG, "syncConversations error: " + code + " - " + message);
                executor.execute(() -> {
                    try {
                        List<ConversationEntity> localData = conversationDao.getAllConversationsSync();
                        allConversations.postValue(localData);
                        conversationsLiveData.postValue(localData);
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading local conversations", e);
                    }
                });
            }

            @Override
            public void onException(Exception e) {
                Log.e(TAG, "syncConversations exception: " + e.getMessage(), e);
                executor.execute(() -> {
                    try {
                        List<ConversationEntity> localData = conversationDao.getAllConversationsSync();
                        allConversations.postValue(localData);
                        conversationsLiveData.postValue(localData);
                    } catch (Exception ex) {
                        Log.e(TAG, "Error loading local conversations", ex);
                    }
                });
            }
        });
    }

    private ConversationEntity convertToEntity(Conversation conversation) {
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

    public void searchConversations(String query) {
        if (query == null || query.isEmpty()) {
            conversationsLiveData.postValue(allConversations.getValue());
        } else {
            List<ConversationEntity> filtered = new java.util.ArrayList<>();
            if (allConversations.getValue() != null) {
                for (ConversationEntity c : allConversations.getValue()) {
                    if (c.name.contains(query) || c.description.contains(query)) {
                        filtered.add(c);
                    }
                }
            }
            conversationsLiveData.postValue(filtered);
        }
    }

    public void createConversation(String name, String description, ConversationRepository.ResultCallback<Void> callback) {
        conversationRepository.createConversation(name, description, new ConversationRepository.ResultCallback<Conversation>() {
            @Override
            public void onSuccess(Conversation data) {
                callback.onSuccess(null);
            }

            @Override
            public void onError(int code, String message) {
                callback.onError(code, message);
            }

            @Override
            public void onException(Exception e) {
                callback.onException(e);
            }
        });
    }

    public void deleteConversation(String id, ConversationRepository.ResultCallback<Void> callback) {
        conversationRepository.deleteConversation(id, callback);
    }

    public void togglePin(String id, boolean isPinned) {
        conversationRepository.togglePin(id, isPinned);
    }
}
