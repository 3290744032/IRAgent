package com.suiyuan.iragent_app.ui.screens.study;

import android.app.Application;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.room.Room;

import com.suiyuan.iragent_app.data.local.AppDatabase;
import com.suiyuan.iragent_app.data.local.ConversationDao;
import com.suiyuan.iragent_app.data.local.MessageDao;
import com.suiyuan.iragent_app.data.remote.ApiService;
import com.suiyuan.iragent_app.data.remote.NetworkClient;
import com.suiyuan.iragent_app.data.repository.ChatRepository;
import com.suiyuan.iragent_app.data.repository.ConversationRepository;
import com.suiyuan.iragent_app.data.model.Conversation;
import com.suiyuan.iragent_app.data.model.Message;
import com.suiyuan.iragent_app.data.model.ResponseSegment;
import com.suiyuan.iragent_app.data.model.TimelineTitleResponse;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

public class StudyViewModel extends AndroidViewModel {

    private final ChatRepository chatRepository;
    private final ConversationRepository conversationRepository;
    private final MutableLiveData<List<ResponseSegment>> segmentsLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> conversationIdLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<List<Conversation>> conversationsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Message>> historyMessagesLiveData = new MutableLiveData<>();

    // 流式响应 LiveData
    private final StringBuilder streamBuffer = new StringBuilder();
    private final MutableLiveData<String> streamTextLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> streamDesmosLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> streamPlot3DLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> streamTimelineLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> streamStartLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> streamDoneLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> timelineTitleLiveData = new MutableLiveData<>();

    private String currentConversationId = "";

    public StudyViewModel(@NonNull Application application) {
        super(application);
        ApiService apiService = NetworkClient.getApiService();
        
        AppDatabase db = Room.databaseBuilder(application, AppDatabase.class, "iragent_db").build();
        ConversationDao conversationDao = db.conversationDao();
        MessageDao messageDao = db.messageDao();
        
        chatRepository = new ChatRepository(apiService, messageDao);
        conversationRepository = new ConversationRepository(apiService, conversationDao);
    }

    public LiveData<List<ResponseSegment>> getSegmentsLiveData() {
        return segmentsLiveData;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public LiveData<String> getConversationIdLiveData() {
        return conversationIdLiveData;
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    public LiveData<List<Conversation>> getConversationsLiveData() {
        return conversationsLiveData;
    }

    public LiveData<List<Message>> getHistoryMessagesLiveData() {
        return historyMessagesLiveData;
    }

    public LiveData<String> getStreamTextLiveData() {
        return streamTextLiveData;
    }

    public LiveData<String> getStreamDesmosLiveData() {
        return streamDesmosLiveData;
    }

    public LiveData<String> getStreamPlot3DLiveData() {
        return streamPlot3DLiveData;
    }

    public LiveData<String> getStreamTimelineLiveData() {
        return streamTimelineLiveData;
    }

    public LiveData<Boolean> getStreamStartLiveData() {
        return streamStartLiveData;
    }

    public LiveData<Boolean> getStreamDoneLiveData() {
        return streamDoneLiveData;
    }

    public void loadConversations() {
        conversationRepository.syncConversations(new ConversationRepository.ResultCallback<List<Conversation>>() {
            @Override
            public void onSuccess(List<Conversation> data) {
                android.util.Log.d("StudyViewModel", "API加载会话成功: count=" + data.size());
                conversationsLiveData.postValue(data);
            }

            @Override
            public void onError(int code, String message) {
                android.util.Log.e("StudyViewModel", "API加载会话失败: code=" + code + ", msg=" + message + "，降级到本地");
                conversationRepository.getAllConversations(new ConversationRepository.ResultCallback<List<Conversation>>() {
                    @Override
                    public void onSuccess(List<Conversation> data) {
                        conversationsLiveData.postValue(data);
                    }

                    @Override
                    public void onError(int code, String message) {
                        errorLiveData.postValue(formatErrorMessage(code, message));
                    }

                    @Override
                    public void onException(Exception e) {
                        errorLiveData.postValue(formatExceptionMessage(e));
                    }
                });
            }

            @Override
            public void onException(Exception e) {
                android.util.Log.e("StudyViewModel", "API加载会话异常: " + e.getMessage() + "，降级到本地");
                conversationRepository.getAllConversations(new ConversationRepository.ResultCallback<List<Conversation>>() {
                    @Override
                    public void onSuccess(List<Conversation> data) {
                        conversationsLiveData.postValue(data);
                    }

                    @Override
                    public void onError(int code, String message) {
                        errorLiveData.postValue(formatErrorMessage(code, message));
                    }

                    @Override
                    public void onException(Exception e) {
                        errorLiveData.postValue(formatExceptionMessage(e));
                    }
                });
            }
        });
    }

    public void setCurrentConversation(String conversationId) {
        this.currentConversationId = conversationId;
        conversationIdLiveData.postValue(conversationId);
        loadHistoryMessages(conversationId);
    }

    public void startNewConversation() {
        currentConversationId = "";
        historyMessagesLiveData.postValue(new ArrayList<>());
        segmentsLiveData.postValue(new ArrayList<>());
        conversationIdLiveData.postValue("");
    }

    public void loadHistoryMessages(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            return;
        }

        isLoadingLiveData.setValue(true);
        chatRepository.getMessagesFromServer(conversationId, new ChatRepository.ResultCallback<List<Message>>() {
            @Override
            public void onSuccess(List<Message> data) {
                historyMessagesLiveData.postValue(data);
                isLoadingLiveData.postValue(false);
            }

            @Override
            public void onError(int code, String message) {
                historyMessagesLiveData.postValue(new ArrayList<>());
                isLoadingLiveData.postValue(false);
            }

            @Override
            public void onException(Exception e) {
                historyMessagesLiveData.postValue(new ArrayList<>());
                isLoadingLiveData.postValue(false);
            }
        });
    }

    public void createConversation(String name) {
        isLoadingLiveData.setValue(true);
        conversationRepository.createConversation(name, "", new ConversationRepository.ResultCallback<Conversation>() {
            @Override
            public void onSuccess(Conversation data) {
                currentConversationId = data.getConversationId();
                conversationIdLiveData.postValue(currentConversationId);
                isLoadingLiveData.postValue(false);
            }

            @Override
            public void onError(int code, String message) {
                errorLiveData.postValue(formatErrorMessage(code, message));
                isLoadingLiveData.postValue(false);
            }

            @Override
            public void onException(Exception e) {
                errorLiveData.postValue(formatExceptionMessage(e));
                isLoadingLiveData.postValue(false);
            }
        });
    }

    public void deleteConversation(String conversationId, Runnable onSuccess) {
        conversationRepository.deleteConversation(conversationId, new ConversationRepository.ResultCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                loadConversations();
                if (onSuccess != null) onSuccess.run();
            }

            @Override
            public void onError(int code, String message) {
                errorLiveData.postValue(formatErrorMessage(code, message));
            }

            @Override
            public void onException(Exception e) {
                errorLiveData.postValue(formatExceptionMessage(e));
            }
        });
    }

    public void getConversationDetail(String conversationId) {
        conversationRepository.getConversationFromServer(conversationId, new ConversationRepository.ResultCallback<Conversation>() {
            @Override
            public void onSuccess(Conversation data) {
                // 刷新当前会话信息
                if (data != null && data.getConversationId().equals(currentConversationId)) {
                    conversationIdLiveData.postValue(currentConversationId);
                }
            }

            @Override
            public void onError(int code, String message) {
                // 静默处理，不影响用户操作
                android.util.Log.w("StudyViewModel", "获取会话详情失败: " + message);
            }

            @Override
            public void onException(Exception e) {
                android.util.Log.w("StudyViewModel", "获取会话详情异常: " + e.getMessage());
            }
        });
    }

    public void solveStream(String problem) {
        if (currentConversationId.isEmpty()) {
            createConversationAndSolveStream(problem);
        } else {
            doSolveStream(problem);
        }
    }

    private void createConversationAndSolveStream(String problem) {
        isLoadingLiveData.setValue(true);
        conversationRepository.createConversation("AI学习", "", new ConversationRepository.ResultCallback<Conversation>() {
            @Override
            public void onSuccess(Conversation data) {
                currentConversationId = data.getConversationId();
                isLoadingLiveData.postValue(false);
                doSolveStream(problem);
            }

            @Override
            public void onError(int code, String message) {
                errorLiveData.postValue(formatErrorMessage(code, message));
                isLoadingLiveData.postValue(false);
            }

            @Override
            public void onException(Exception e) {
                errorLiveData.postValue(formatExceptionMessage(e));
                isLoadingLiveData.postValue(false);
            }
        });
    }

    private void doSolveStream(String problem) {
        chatRepository.solveStream(problem, currentConversationId, new ChatRepository.StreamCallback() {
            @Override
            public void onStart() {
                // 清空缓冲区
                streamBuffer.setLength(0);
                streamStartLiveData.postValue(true);
            }

            @Override
            public void onText(String text) {
                if (text != null) {
                    // 在 ViewModel 中拼接字符串
                    streamBuffer.append(text);
                    // 发送完整内容
                    streamTextLiveData.postValue(streamBuffer.toString());
                }
            }

            @Override
            public void onGeogebra(String expression) {
                streamDesmosLiveData.postValue(expression);
            }

            @Override
            public void onPlot3D(String config) {
                streamPlot3DLiveData.postValue(config);
            }

            @Override
            public void onTimeline(String json) {
                streamTimelineLiveData.postValue(json);
            }

            @Override
            public void onDone() {
                streamDoneLiveData.postValue(true);
            }

            @Override
            public void onError(int code, String message) {
                errorLiveData.postValue(formatErrorMessage(code, message));
            }

            @Override
            public void onException(Exception e) {
                errorLiveData.postValue(formatExceptionMessage(e));
            }
        });

        // 在流式聊天后调用 /timeline/title 接口生成标题
        chatRepository.generateTimelineTitle(problem, currentConversationId, new ChatRepository.ResultCallback<TimelineTitleResponse>() {
            @Override
            public void onSuccess(TimelineTitleResponse data) {
                android.util.Log.d("StudyViewModel", "Timeline title: " + data.getTitle() + ", saved: " + data.isSaved());
            }

            @Override
            public void onError(int code, String message) {
                android.util.Log.w("StudyViewModel", "Failed to generate timeline title: code=" + code + ", msg=" + message);
            }

            @Override
            public void onException(Exception e) {
                android.util.Log.w("StudyViewModel", "Exception generating timeline title: " + e.getMessage());
            }
        });
    }

    private String formatErrorMessage(int code, String message) {
        if (code >= 500) {
            return "服务器内部错误，请稍后重试";
        } else if (code == 404) {
            return "请求的资源未找到";
        } else if (code == 401) {
            return "401-未授权，请重新登录";
        } else if (code == 403) {
            return "访问被拒绝";
        } else if (code == 400) {
            return "请求参数错误";
        } else {
            return "请求失败 [" + code + "]: " + message;
        }
    }

    private String formatExceptionMessage(Exception e) {
        if (e.getCause() instanceof SocketTimeoutException || e.getMessage() != null && e.getMessage().contains("timeout")) {
            return "网络超时，请检查网络连接或稍后重试";
        } else if (e.getCause() instanceof IOException || e.getMessage() != null && e.getMessage().contains("IOException")) {
            return "网络异常，请检查网络连接";
        } else if (!isNetworkAvailable()) {
            return "网络连接失败，请检查网络设置";
        } else {
            return "服务器内部错误，请稍后重试";
        }
    }

    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) getApplication().getSystemService(Application.CONNECTIVITY_SERVICE);
            if (connectivityManager == null) {
                return false;
            }
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        } catch (SecurityException e) {
            return false;
        }
    }

    public void resetConversation() {
        currentConversationId = "";
    }
}