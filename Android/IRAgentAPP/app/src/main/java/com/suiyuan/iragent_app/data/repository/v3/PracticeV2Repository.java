package com.suiyuan.iragent_app.data.repository.v3;

import com.suiyuan.iragent_app.data.model.ApiResponse;
import com.suiyuan.iragent_app.data.model.v3.*;
import com.suiyuan.iragent_app.data.remote.v3.ApiServiceV3;
import com.suiyuan.iragent_app.data.remote.v3.NetworkClientV3;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PracticeV2Repository {

    private final ApiServiceV3 apiService;

    public PracticeV2Repository() {
        this.apiService = NetworkClientV3.getApiService();
    }

    public interface ResultCallback<T> {
        void onSuccess(T data);
        void onError(int code, String message);
        void onException(Exception e);
    }

    private <T> void enqueue(Call<ApiResponse<T>> call, ResultCallback<T> callback) {
        call.enqueue(new Callback<ApiResponse<T>>() {
            @Override
            public void onResponse(Call<ApiResponse<T>> c, Response<ApiResponse<T>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<T> body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        callback.onSuccess(body.getData());
                    } else {
                        callback.onError(body.getCode(), body.getMessage());
                    }
                } else {
                    callback.onError(response.code(), response.message());
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<T>> c, Throwable t) {
                callback.onException(new Exception(t));
            }
        });
    }

    public void listExamArchive(String subject, Integer year, String examType,
                                String knowledgePoint, Integer difficulty, int page, int size,
                                ResultCallback<List<ExamQuestion>> callback) {
        enqueue(apiService.listExamArchive(subject, year, examType, knowledgePoint, difficulty, page, size), callback);
    }

    public void getExamFilters(ResultCallback<ExamFilterData> callback) {
        enqueue(apiService.getExamFilters(), callback);
    }

    public void simulateExamArchive(SimulateExamRequest body, ResultCallback<List<ExamQuestion>> callback) {
        enqueue(apiService.simulateExamArchive(body), callback);
    }

    public void submitQuestionFeedback(QuestionFeedbackRequest body, ResultCallback<Map<String, Object>> callback) {
        enqueue(apiService.submitQuestionFeedback(body), callback);
    }

    public void getDailyPractice(String subject, int count, ResultCallback<DailyPracticeSession> callback) {
        enqueue(apiService.getDailyPractice(subject, count), callback);
    }

    public void submitDailyPractice(SubmitAnswerRequest body, ResultCallback<SubmitAnswerResult> callback) {
        enqueue(apiService.submitDailyPractice(body), callback);
    }

    public void generateSmartPaper(SmartPaperRequest body, ResultCallback<SmartPaper> callback) {
        enqueue(apiService.generateSmartPaper(body), callback);
    }

    public void submitPaper(SubmitAnswerRequest body, ResultCallback<SubmitAnswerResult> callback) {
        enqueue(apiService.submitPaper(body), callback);
    }
}
