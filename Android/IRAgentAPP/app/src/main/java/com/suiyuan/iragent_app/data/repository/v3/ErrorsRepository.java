package com.suiyuan.iragent_app.data.repository.v3;

import com.suiyuan.iragent_app.data.model.ApiResponse;
import com.suiyuan.iragent_app.data.model.v3.*;
import com.suiyuan.iragent_app.data.remote.v3.ApiServiceV3;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ErrorsRepository {

    private final ApiServiceV3 apiService;

    public ErrorsRepository(ApiServiceV3 apiService) {
        this.apiService = apiService;
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

    public void listErrors(String subject, String errorType, int page, int size,
                           ResultCallback<List<ErrorItem>> callback) {
        apiService.listErrors(subject, errorType, page, size)
                .enqueue(createCallback(callback, ApiResponse::getData));
    }

    public void getErrorDetail(String id, ResultCallback<ErrorDetail> callback) {
        apiService.getErrorDetail(id).enqueue(createCallback(callback, ApiResponse::getData));
    }

    public void getReviewQueue(ResultCallback<List<ReviewItem>> callback) {
        apiService.getReviewQueue().enqueue(createCallback(callback, ApiResponse::getData));
    }

    public void markMastered(String id, ResultCallback<Boolean> callback) {
        apiService.markMastered(id).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                                   Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(true);
                } else if (response.body() != null) {
                    callback.onError(response.body().getCode(), response.body().getMessage());
                } else {
                    callback.onError(response.code(), response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                callback.onException(new Exception(t));
            }
        });
    }

    public void getSimilarQuestions(String id, ResultCallback<List<SimilarQuestion>> callback) {
        apiService.getSimilarQuestions(id).enqueue(createCallback(callback, ApiResponse::getData));
    }
}
