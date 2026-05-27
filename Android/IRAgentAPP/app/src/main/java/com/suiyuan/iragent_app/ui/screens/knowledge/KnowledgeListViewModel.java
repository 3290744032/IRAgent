package com.suiyuan.iragent_app.ui.screens.knowledge;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.suiyuan.iragent_app.data.model.v3.NoteFragment;
import com.suiyuan.iragent_app.data.model.v3.NoteItem;
import com.suiyuan.iragent_app.data.model.v3.UploadResult;
import com.suiyuan.iragent_app.data.remote.v3.ApiServiceV3;
import com.suiyuan.iragent_app.data.remote.v3.NetworkClientV3;
import com.suiyuan.iragent_app.data.repository.v3.KnowledgeRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class KnowledgeListViewModel extends AndroidViewModel {

    private final KnowledgeRepository repository;

    private final MutableLiveData<List<NoteItem>> notesList = new MutableLiveData<>();
    private final MutableLiveData<List<NoteFragment>> searchResults = new MutableLiveData<>();
    private final MutableLiveData<UploadResult> uploadResult = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public KnowledgeListViewModel(@NonNull Application application) {
        super(application);
        ApiServiceV3 apiService = NetworkClientV3.getApiService();
        this.repository = new KnowledgeRepository(apiService);
    }

    public MutableLiveData<List<NoteItem>> getNotesList() { return notesList; }
    public MutableLiveData<List<NoteFragment>> getSearchResults() { return searchResults; }
    public MutableLiveData<UploadResult> getUploadResult() { return uploadResult; }
    public MutableLiveData<String> getError() { return error; }
    public MutableLiveData<Boolean> getIsLoading() { return isLoading; }

    public void clearError() { error.postValue(null); }

    public void listNotes(String subject, int page, int size) {
        isLoading.postValue(true);
        repository.listNotes(subject, page, size, new KnowledgeRepository.ResultCallback<List<NoteItem>>() {
            @Override
            public void onSuccess(List<NoteItem> data) {
                isLoading.postValue(false);
                notesList.postValue(data);
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

    public void searchNotes(String query, int topK) {
        isLoading.postValue(true);
        repository.searchNotes(query, topK, new KnowledgeRepository.ResultCallback<List<NoteFragment>>() {
            @Override
            public void onSuccess(List<NoteFragment> data) {
                isLoading.postValue(false);
                searchResults.postValue(data);
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

    public void uploadNote(android.net.Uri fileUri) {
        isLoading.postValue(true);
        try {
            InputStream is = getApplication().getContentResolver().openInputStream(fileUri);
            if (is == null) {
                error.postValue("无法读取文件");
                isLoading.postValue(false);
                return;
            }

            File tempFile = new File(getApplication().getCacheDir(), "upload_note");
            FileOutputStream fos = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            fos.close();
            is.close();

            RequestBody requestFile = RequestBody.create(tempFile, MediaType.parse("application/octet-stream"));
            MultipartBody.Part part = MultipartBody.Part.createFormData("file", "note", requestFile);

            repository.uploadNote(part, new KnowledgeRepository.ResultCallback<UploadResult>() {
                @Override
                public void onSuccess(UploadResult data) {
                    isLoading.postValue(false);
                    uploadResult.postValue(data);
                    listNotes("", 0, 20);
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
        } catch (Exception e) {
            isLoading.postValue(false);
            error.postValue(e.getMessage());
        }
    }
}
