package com.suiyuan.iragent_app.data.repository.v3;

import com.suiyuan.iragent_app.data.model.ApiResponse;
import com.suiyuan.iragent_app.data.model.v3.*;
import com.suiyuan.iragent_app.data.remote.v3.ApiServiceV3;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardRepository {

    private final ApiServiceV3 apiService;

    public DashboardRepository(ApiServiceV3 apiService) {
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

    public void getOverview(ResultCallback<DashboardOverview> callback) {
        apiService.getOverview().enqueue(createCallback(callback, ApiResponse::getData));
    }

    public void getMasteryRadar(ResultCallback<MasteryRadarData> callback) {
        apiService.getMasteryRadar().enqueue(createCallback(callback, ApiResponse::getData));
    }

    public void getTodayTasks(ResultCallback<List<TaskItem>> callback) {
        apiService.getTodayTasks().enqueue(createCallback(callback, ApiResponse::getData));
    }

    public void getWeeklyReport(ResultCallback<WeeklyReport> callback) {
        apiService.getWeeklyReport().enqueue(createCallback(callback, ApiResponse::getData));
    }
}
