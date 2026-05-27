package com.suiyuan.iragent_app.ui.screens.knowledge;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.suiyuan.iragent_app.data.model.v3.NoteDetail;
import com.suiyuan.iragent_app.data.remote.v3.ApiServiceV3;
import com.suiyuan.iragent_app.data.remote.v3.NetworkClientV3;
import com.suiyuan.iragent_app.data.repository.v3.KnowledgeRepository;

import java.util.Map;

public class KnowledgeDetailViewModel extends AndroidViewModel {

    private final KnowledgeRepository repository;

    private final MutableLiveData<NoteDetail> noteDetail = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> optimizedContent = new MutableLiveData<>();

    public KnowledgeDetailViewModel(@NonNull Application application) {
        super(application);
        ApiServiceV3 apiService = NetworkClientV3.getApiService();
        this.repository = new KnowledgeRepository(apiService);
    }

    public MutableLiveData<NoteDetail> getNoteDetail() { return noteDetail; }
    public MutableLiveData<String> getError() { return error; }
    public MutableLiveData<Boolean> getIsLoading() { return isLoading; }
    public MutableLiveData<String> getOptimizedContent() { return optimizedContent; }

    public void loadNoteDetail(String noteId) {
        isLoading.postValue(true);
        repository.getNoteDetail(noteId, new KnowledgeRepository.ResultCallback<NoteDetail>() {
            @Override
            public void onSuccess(NoteDetail data) {
                isLoading.postValue(false);
                noteDetail.postValue(data);
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

    public void updateNote(String noteId, java.util.Map<String, String> body) {
        repository.updateNote(noteId, body, new KnowledgeRepository.ResultCallback<Boolean>() {
            @Override public void onSuccess(Boolean ok) { loadNoteDetail(noteId); }
            @Override public void onError(int code, String msg) { error.postValue("保存失败: " + msg); }
            @Override public void onException(Exception e) { error.postValue(e.getMessage()); }
        });
    }

    public void optimizeNote(String noteId) {
        isLoading.postValue(true);
        repository.optimizeNote(noteId, new KnowledgeRepository.ResultCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                isLoading.postValue(false);
                if (data != null && Boolean.TRUE.equals(data.get("optimized"))) {
                    String content = (String) data.get("content");
                    if (content != null) {
                        optimizedContent.postValue(content);
                        loadNoteDetail(noteId);
                    }
                } else {
                    error.postValue("AI 优化失败");
                }
            }
            @Override
            public void onError(int code, String msg) {
                isLoading.postValue(false);
                error.postValue("AI 优化失败: " + msg);
            }
            @Override
            public void onException(Exception e) {
                isLoading.postValue(false);
                error.postValue(e.getMessage());
            }
        });
    }
}
