package com.suiyuan.iragent_app.ui.screens.errors;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.suiyuan.iragent_app.data.model.v3.ErrorDetail;
import com.suiyuan.iragent_app.data.model.v3.QuestionFeedbackRequest;
import com.suiyuan.iragent_app.data.model.v3.SimilarQuestion;
import com.suiyuan.iragent_app.data.remote.v3.ApiServiceV3;
import com.suiyuan.iragent_app.data.remote.v3.NetworkClientV3;
import com.suiyuan.iragent_app.data.repository.v3.ErrorsRepository;
import com.suiyuan.iragent_app.data.repository.v3.PracticeV2Repository;

import java.util.List;
import java.util.Map;

public class ErrorsDetailViewModel extends AndroidViewModel {

    private final ErrorsRepository repository;
    private final PracticeV2Repository practiceRepository;

    private final MutableLiveData<ErrorDetail> errorDetail = new MutableLiveData<>();
    private final MutableLiveData<Boolean> markMasteredResult = new MutableLiveData<>();
    private final MutableLiveData<List<SimilarQuestion>> similarQuestions = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public ErrorsDetailViewModel(@NonNull Application application) {
        super(application);
        ApiServiceV3 apiService = NetworkClientV3.getApiService();
        this.repository = new ErrorsRepository(apiService);
        this.practiceRepository = new PracticeV2Repository();
    }

    public MutableLiveData<ErrorDetail> getErrorDetail() { return errorDetail; }
    public MutableLiveData<Boolean> getMarkMasteredResult() { return markMasteredResult; }
    public MutableLiveData<List<SimilarQuestion>> getSimilarQuestions() { return similarQuestions; }
    public MutableLiveData<String> getError() { return error; }
    public MutableLiveData<Boolean> getIsLoading() { return isLoading; }

    public void loadErrorDetail(String id) {
        isLoading.postValue(true);
        repository.getErrorDetail(id, new ErrorsRepository.ResultCallback<ErrorDetail>() {
            @Override
            public void onSuccess(ErrorDetail data) {
                isLoading.postValue(false);
                errorDetail.postValue(data);
            }
            @Override
            public void onError(int code, String message) {
                isLoading.postValue(false);
                error.postValue(message);
            }
            @Override
            public void onException(Exception e) {
                isLoading.postValue(false);
                error.postValue(e.getMessage());
            }
        });
    }

    public void markMastered(String id) {
        repository.markMastered(id, new ErrorsRepository.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean data) { markMasteredResult.postValue(data); }
            @Override
            public void onError(int code, String message) { error.postValue(message); }
            @Override
            public void onException(Exception e) { error.postValue(e.getMessage()); }
        });
    }

    public void loadSimilarQuestions(String id) {
        repository.getSimilarQuestions(id, new ErrorsRepository.ResultCallback<List<SimilarQuestion>>() {
            @Override
            public void onSuccess(List<SimilarQuestion> data) { similarQuestions.postValue(data); }
            @Override
            public void onError(int code, String message) { error.postValue(message); }
            @Override
            public void onException(Exception e) { error.postValue(e.getMessage()); }
        });
    }

    public void submitFeedback(String questionId, String feedbackType, String comment) {
        practiceRepository.submitQuestionFeedback(
                new QuestionFeedbackRequest(questionId, feedbackType, comment),
                new PracticeV2Repository.ResultCallback<Map<String, Object>>() {
                    @Override public void onSuccess(Map<String, Object> data) { error.postValue("反馈已提交，感谢你的帮助！"); }
                    @Override public void onError(int code, String message) { error.postValue("反馈提交失败: " + message); }
                    @Override public void onException(Exception e) { error.postValue("反馈提交异常: " + e.getMessage()); }
                });
    }
}
