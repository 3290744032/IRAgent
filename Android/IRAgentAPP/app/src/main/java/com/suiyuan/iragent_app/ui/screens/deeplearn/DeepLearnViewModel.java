package com.suiyuan.iragent_app.ui.screens.deeplearn;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.suiyuan.iragent_app.data.model.v2.*;
import com.suiyuan.iragent_app.data.repository.v2.DeepLearnRepository;

import java.util.List;

public class DeepLearnViewModel extends AndroidViewModel {

    private static final String TAG = "DeepLearnViewModel";

    private final DeepLearnRepository repository;

    private final MutableLiveData<SessionResponse> sessionLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> teachContentLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isTeachingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<SessionSummaryResponse> summaryLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> summaryContentLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> timelineLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSessionCompleteLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<List<SessionHistoryItem>> historyLiveData = new MutableLiveData<>();

    private SessionResponse session;

    public DeepLearnViewModel(@NonNull Application application) {
        super(application);
        this.repository = new DeepLearnRepository();
    }

    public LiveData<SessionResponse> getSessionLiveData() { return sessionLiveData; }
    public LiveData<String> getTeachContentLiveData() { return teachContentLiveData; }
    public LiveData<Boolean> getIsTeachingLiveData() { return isTeachingLiveData; }
    public LiveData<Boolean> getIsLoadingLiveData() { return isLoadingLiveData; }
    public LiveData<String> getErrorLiveData() { return errorLiveData; }
    public LiveData<SessionSummaryResponse> getSummaryLiveData() { return summaryLiveData; }
    public LiveData<String> getSummaryContentLiveData() { return summaryContentLiveData; }
    public LiveData<String> getTimelineLiveData() { return timelineLiveData; }
    public LiveData<List<SessionHistoryItem>> getHistoryLiveData() { return historyLiveData; }
    public LiveData<Boolean> getIsSessionCompleteLiveData() { return isSessionCompleteLiveData; }

    public SessionResponse getSession() { return session; }

    public void createSession(String question, String subjectType) {
        isLoadingLiveData.setValue(true);
        repository.createSession(question, subjectType, new DeepLearnRepository.ResultCallback<SessionResponse>() {
            @Override
            public void onSuccess(SessionResponse data) {
                session = data;
                sessionLiveData.postValue(data);
                isLoadingLiveData.postValue(false);
                android.util.Log.d(TAG, "Session created: id=" + data.getSessionId());
                teachStep("interactive");
            }

            @Override
            public void onError(int code, String message) {
                errorLiveData.postValue(formatError(code, message));
                isLoadingLiveData.postValue(false);
            }

            @Override
            public void onException(Exception e) {
                errorLiveData.postValue(e.getMessage());
                isLoadingLiveData.postValue(false);
            }
        });
    }

    public void loadSessionDetail(String sessionId) {
        isLoadingLiveData.setValue(true);
        isSessionCompleteLiveData.postValue(false);
        repository.getSessionDetail(sessionId, new DeepLearnRepository.ResultCallback<SessionResponse>() {
            @Override
            public void onSuccess(SessionResponse data) {
                session = data;
                sessionLiveData.postValue(data);
                isLoadingLiveData.postValue(false);
            }

            @Override
            public void onError(int code, String message) {
                errorLiveData.postValue(formatError(code, message));
                isLoadingLiveData.postValue(false);
            }

            @Override
            public void onException(Exception e) {
                errorLiveData.postValue(e.getMessage());
                isLoadingLiveData.postValue(false);
            }
        });
    }

    public void teachStep(String mode) {
        if (session == null) {
            android.util.Log.w(TAG, "teachStep skipped: session is null");
            return;
        }
        android.util.Log.d(TAG, "teachStep: sessionId=" + session.getSessionId() + ", mode=" + mode);
        teachContentLiveData.postValue("");
        isSessionCompleteLiveData.postValue(false);
        isTeachingLiveData.postValue(true);

        repository.teachStream(session.getSessionId(), mode, new DeepLearnRepository.TeachCallback() {
            @Override
            public void onStart(String message) {
                android.util.Log.d(TAG, "onStart: message=" + message);
                teachContentLiveData.setValue("");
            }

            @Override
            public void onContent(String text) {
                String current = teachContentLiveData.getValue();
                teachContentLiveData.setValue(current != null ? current + text : text);
            }

            @Override
            public void onTimeline(String timelineJson) {
                android.util.Log.d(TAG, "onTimeline: json length=" + timelineJson.length());
                timelineLiveData.postValue(timelineJson);
            }

            @Override
            public void onDone() {
                String accumulated = teachContentLiveData.getValue();
                int len = accumulated != null ? accumulated.length() : 0;
                android.util.Log.d(TAG, "onDone: teach stream completed, accumulated chars=" + len);
                if (accumulated != null && !accumulated.isEmpty()) {
                    int chunkSize = 3000;
                    for (int i = 0; i < accumulated.length(); i += chunkSize) {
                        int end = Math.min(i + chunkSize, accumulated.length());
                        android.util.Log.d(TAG, "TEACH_FULL[" + i + "-" + end + "]: " + accumulated.substring(i, end));
                    }
                }
                isTeachingLiveData.setValue(false);
            }

            @Override
            public void onCompleted() {
                android.util.Log.d(TAG, "onCompleted: session completed, loading summary");
                isSessionCompleteLiveData.setValue(true);
                isTeachingLiveData.setValue(false);
                loadSummary();
            }

            @Override
            public void onError(int code, String message) {
                android.util.Log.e(TAG, "onError: code=" + code + ", message=" + message);
                isTeachingLiveData.setValue(false);
                errorLiveData.postValue(formatError(code, message));
            }

            @Override
            public void onException(Exception e) {
                android.util.Log.e(TAG, "onException: " + e.getMessage(), e);
                isTeachingLiveData.setValue(false);
                errorLiveData.postValue(e.getMessage());
            }
        });
    }

    public void submitAnswer(String answer) {
        if (session == null) {
            android.util.Log.w(TAG, "submitAnswer skipped: session is null");
            return;
        }
        android.util.Log.d(TAG, "submitAnswer: sessionId=" + session.getSessionId() + ", answer=" + answer);
        isLoadingLiveData.setValue(true);

        repository.submitAnswer(session.getSessionId(), answer, new DeepLearnRepository.ResultCallback<AnswerResponse>() {
            @Override
            public void onSuccess(AnswerResponse data) {
                android.util.Log.d(TAG, "submitAnswer success, feedbackToken=" + data.getFeedbackToken());
                isLoadingLiveData.postValue(false);
                teachStep("interactive");
            }

            @Override
            public void onError(int code, String message) {
                android.util.Log.e(TAG, "submitAnswer error: " + code + " " + message);
                isLoadingLiveData.postValue(false);
                errorLiveData.postValue(formatError(code, message));
            }

            @Override
            public void onException(Exception e) {
                android.util.Log.e(TAG, "submitAnswer exception: " + e.getMessage(), e);
                isLoadingLiveData.postValue(false);
                errorLiveData.postValue(e.getMessage());
            }
        });
    }

    public void loadSummary() {
        if (session == null) return;
        android.util.Log.d(TAG, "loadSummary: background generating summary for session " + session.getSessionId());
        summaryContentLiveData.setValue("");
        repository.summaryStream(session.getSessionId(), new DeepLearnRepository.SummaryCallback() {
            @Override
            public void onStart(String message) {
                android.util.Log.d(TAG, "summaryStream onStart: " + message);
            }

            @Override
            public void onContent(String text) {
                String current = summaryContentLiveData.getValue();
                summaryContentLiveData.setValue(current != null ? current + text : text);
            }

            @Override
            public void onSummaryData(SessionSummaryResponse data) {
                summaryLiveData.postValue(data);
            }

            @Override
            public void onError(int code, String message) {
                errorLiveData.postValue(message);
            }

            @Override
            public void onException(Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        });
    }

    public void generateTimelineSync(String topic) {
        isLoadingLiveData.setValue(true);
        android.util.Log.d(TAG, "generateTimelineSync: topic=" + topic);
        repository.generateTimelineSync(topic, new DeepLearnRepository.ResultCallback<String>() {
            @Override
            public void onSuccess(String timelineJson) {
                android.util.Log.d(TAG, "generateTimelineSync success, json length=" + timelineJson.length());
                isLoadingLiveData.postValue(false);
                timelineLiveData.postValue(timelineJson);
            }

            @Override
            public void onError(int code, String message) {
                android.util.Log.e(TAG, "generateTimelineSync error: " + code + " " + message);
                isLoadingLiveData.postValue(false);
                errorLiveData.postValue(message);
            }

            @Override
            public void onException(Exception e) {
                android.util.Log.e(TAG, "generateTimelineSync exception", e);
                isLoadingLiveData.postValue(false);
                errorLiveData.postValue(e.getMessage());
            }
        });
    }

    public void loadHistory(int page, int size) {
        isLoadingLiveData.setValue(true);
        repository.getSessionHistory(page, size, new DeepLearnRepository.ResultCallback<SessionHistoryResponse>() {
            @Override
            public void onSuccess(SessionHistoryResponse data) {
                historyLiveData.postValue(data != null ? data.getSessions() : null);
                isLoadingLiveData.postValue(false);
            }

            @Override
            public void onError(int code, String message) {
                errorLiveData.postValue(formatError(code, message));
                isLoadingLiveData.postValue(false);
            }

            @Override
            public void onException(Exception e) {
                errorLiveData.postValue(e.getMessage());
                isLoadingLiveData.postValue(false);
            }
        });
    }

    public void deleteSession(String sessionId) {
        isLoadingLiveData.setValue(true);
        repository.deleteSession(sessionId, new DeepLearnRepository.ResultCallback<DeleteSessionResponse>() {
            @Override
            public void onSuccess(DeleteSessionResponse data) {
                loadHistory(1, 20);
            }

            @Override
            public void onError(int code, String message) {
                errorLiveData.postValue(formatError(code, message));
                isLoadingLiveData.postValue(false);
            }

            @Override
            public void onException(Exception e) {
                errorLiveData.postValue(e.getMessage());
                isLoadingLiveData.postValue(false);
            }
        });
    }

    private String formatError(int code, String message) {
        if (code >= 500) return "服务器内部错误，请稍后重试";
        if (code == 404) return "会话不存在";
        if (code == 401) return "登录已过期，请重新登录";
        if (code == 409) return "状态冲突，请刷新";
        if (code == 429) return "操作过于频繁，请稍后再试";
        return "请求失败 [" + code + "]: " + message;
    }
}
