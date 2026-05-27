package com.suiyuan.iragent_app.data.repository.v3;

import com.suiyuan.iragent_app.data.model.ApiResponse;
import com.suiyuan.iragent_app.data.model.v3.*;
import com.suiyuan.iragent_app.data.remote.v3.ApiServiceV3;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class KnowledgeRepository {

    private final ApiServiceV3 apiService;

    public KnowledgeRepository(ApiServiceV3 apiService) {
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

    public void listNotes(String subject, int page, int size, ResultCallback<List<NoteItem>> callback) {
        apiService.listNotes(subject, page, size).enqueue(createCallback(callback, ApiResponse::getData));
    }

    public void getNoteDetail(String noteId, ResultCallback<NoteDetail> callback) {
        apiService.getNoteDetail(noteId).enqueue(createCallback(callback, ApiResponse::getData));
    }

    public void uploadNote(okhttp3.MultipartBody.Part file, okhttp3.RequestBody title, ResultCallback<UploadResult> callback) {
        apiService.uploadNote(file, title).enqueue(createCallback(callback, ApiResponse::getData));
    }

    public void searchNotes(String query, int topK, ResultCallback<List<NoteFragment>> callback) {
        SearchRequest request = new SearchRequest(query, topK);
        apiService.searchNotes(request).enqueue(createCallback(callback, ApiResponse::getData));
    }

    public void deleteNote(String noteId, ResultCallback<Boolean> callback) {
        apiService.deleteNote(noteId).enqueue(createCallback(callback, r -> r != null));
    }

    public void updateNote(String noteId, java.util.Map<String, String> body, ResultCallback<Boolean> callback) {
        apiService.updateNote(noteId, body).enqueue(createCallback(callback, r -> r != null));
    }

    public void optimizeNote(String noteId, String instruction, ResultCallback<Map<String, Object>> callback) {
        apiService.optimizeNote(noteId, java.util.Map.of("instruction", instruction)).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                                   Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
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
}
