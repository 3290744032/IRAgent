package com.suiyuan.iragent_app.ui.screens.errors;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.suiyuan.iragent_app.data.model.v3.ErrorItem;
import com.suiyuan.iragent_app.data.model.v3.ReviewItem;
import com.suiyuan.iragent_app.data.remote.v3.ApiServiceV3;
import com.suiyuan.iragent_app.data.remote.v3.NetworkClientV3;
import com.suiyuan.iragent_app.data.repository.v3.ErrorsRepository;

import java.util.List;

public class ErrorsListViewModel extends AndroidViewModel {

    private final ErrorsRepository repository;

    private final MutableLiveData<List<ErrorItem>> errorsList = new MutableLiveData<>();
    private final MutableLiveData<List<ReviewItem>> reviewQueue = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public ErrorsListViewModel(@NonNull Application application) {
        super(application);
        ApiServiceV3 apiService = NetworkClientV3.getApiService();
        this.repository = new ErrorsRepository(apiService);
    }

    public MutableLiveData<List<ErrorItem>> getErrorsList() { return errorsList; }
    public MutableLiveData<List<ReviewItem>> getReviewQueue() { return reviewQueue; }
    public MutableLiveData<String> getError() { return error; }
    public MutableLiveData<Boolean> getIsLoading() { return isLoading; }

    public void listErrors(String subject, String errorType, int page, int size) {
        isLoading.postValue(true);
        repository.listErrors(subject, errorType, page, size,
                new ErrorsRepository.ResultCallback<List<ErrorItem>>() {
            @Override
            public void onSuccess(List<ErrorItem> data) {
                isLoading.postValue(false);
                errorsList.postValue(data);
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

    public void loadReviewQueue() {
        repository.getReviewQueue(new ErrorsRepository.ResultCallback<List<ReviewItem>>() {
            @Override
            public void onSuccess(List<ReviewItem> data) {
                reviewQueue.postValue(data);
            }
            @Override
            public void onError(int code, String message) {
                error.postValue(message);
            }
            @Override
            public void onException(Exception e) {
                error.postValue(e.getMessage());
            }
        });
    }
}
