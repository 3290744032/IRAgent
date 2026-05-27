package com.suiyuan.iragent_app.ui.screens.practice;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.suiyuan.iragent_app.data.model.v3.*;
import com.suiyuan.iragent_app.data.repository.v3.PracticeV2Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DailyPracticeViewModel extends AndroidViewModel {

    private final PracticeV2Repository repository;
    private final MutableLiveData<DailyPracticeSession> session = new MutableLiveData<>();
    private final MutableLiveData<SubmitAnswerResult> result = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private final Map<String, String> answerMap = new HashMap<>();
    private final Map<String, Uri> photoUriMap = new HashMap<>();

    public DailyPracticeViewModel(@NonNull Application application) {
        super(application);
        this.repository = new PracticeV2Repository();
    }

    public MutableLiveData<DailyPracticeSession> getSession() { return session; }
    public MutableLiveData<SubmitAnswerResult> getResult() { return result; }
    public MutableLiveData<Boolean> getIsLoading() { return isLoading; }
    public MutableLiveData<String> getError() { return error; }

    public Map<String, String> getAnswerMap() { return answerMap; }
    public Map<String, Uri> getPhotoUriMap() { return photoUriMap; }

    public void loadPractice(String subject, int count) {
        isLoading.postValue(true);
        answerMap.clear();
        photoUriMap.clear();
        repository.getDailyPractice(subject, count,
                new PracticeV2Repository.ResultCallback<DailyPracticeSession>() {
                    @Override public void onSuccess(DailyPracticeSession data) { session.postValue(data); isLoading.postValue(false); }
                    @Override public void onError(int code, String message) { error.postValue(message); isLoading.postValue(false); }
                    @Override public void onException(Exception e) { error.postValue(e.getMessage()); isLoading.postValue(false); }
                });
    }

    public void setAnswer(String questionId, String answer) {
        if (answer == null || answer.isEmpty()) {
            answerMap.remove(questionId);
        } else {
            answerMap.put(questionId, answer);
        }
    }

    public void setPhotoUri(String questionId, Uri uri) {
        if (uri == null) {
            photoUriMap.remove(questionId);
        } else {
            photoUriMap.put(questionId, uri);
        }
    }

    public void submitAll(String sessionId, String source) {
        isLoading.postValue(true);
        List<SubmitAnswerRequest.AnswerEntry> entries = new ArrayList<>();
        for (Map.Entry<String, String> e : answerMap.entrySet()) {
            entries.add(new SubmitAnswerRequest.AnswerEntry(e.getKey(), e.getValue(), 30));
        }
        for (String qId : photoUriMap.keySet()) {
            if (!answerMap.containsKey(qId)) {
                entries.add(new SubmitAnswerRequest.AnswerEntry(qId, "[photo]", 30));
            }
        }
        SubmitAnswerRequest req = new SubmitAnswerRequest(sessionId, source, entries);
        if ("smart_paper".equals(source)) {
            repository.submitPaper(req, new PracticeV2Repository.ResultCallback<SubmitAnswerResult>() {
                @Override public void onSuccess(SubmitAnswerResult data) { result.postValue(data); isLoading.postValue(false); }
                @Override public void onError(int code, String message) { error.postValue(message); isLoading.postValue(false); }
                @Override public void onException(Exception e) { error.postValue(e.getMessage()); isLoading.postValue(false); }
            });
        } else {
            repository.submitDailyPractice(req, new PracticeV2Repository.ResultCallback<SubmitAnswerResult>() {
                @Override public void onSuccess(SubmitAnswerResult data) { result.postValue(data); isLoading.postValue(false); }
                @Override public void onError(int code, String message) { error.postValue(message); isLoading.postValue(false); }
                @Override public void onException(Exception e) { error.postValue(e.getMessage()); isLoading.postValue(false); }
            });
        }
    }

    public void submitDailyFeedback(String questionId, String feedbackType, String comment) {
        repository.submitQuestionFeedback(
                new QuestionFeedbackRequest(questionId, feedbackType, comment),
                new PracticeV2Repository.ResultCallback<Map<String, Object>>() {
                    @Override public void onSuccess(Map<String, Object> data) { error.postValue("反馈已提交，感谢你的帮助！"); }
                    @Override public void onError(int code, String message) { error.postValue("反馈提交失败: " + message); }
                    @Override public void onException(Exception e) { error.postValue("反馈提交异常: " + e.getMessage()); }
                });
    }
}
