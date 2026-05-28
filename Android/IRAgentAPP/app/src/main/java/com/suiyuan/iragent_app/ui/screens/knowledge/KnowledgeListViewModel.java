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
    private final MutableLiveData<String> uploadStep = new MutableLiveData<>();
    private final MutableLiveData<Integer> uploadProgress = new MutableLiveData<>(0);
    private final MutableLiveData<Map<String, Object>> graphData = new MutableLiveData<>();

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
    public MutableLiveData<String> getUploadStep() { return uploadStep; }
    public MutableLiveData<Integer> getUploadProgress() { return uploadProgress; }
    public MutableLiveData<Map<String, Object>> getGraphData() { return graphData; }

    public void clearError() { error.postValue(null); }

    public void loadGraphData() {
        repository.getGraphData(new KnowledgeRepository.ResultCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                graphData.postValue(data);
            }

            @Override
            public void onError(int code, String message) {
                // 图谱加载失败不打扰用户，静默降级
            }

            @Override
            public void onException(Exception e) {
                // 静默降级
            }
        });
    }

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

    public void uploadNote(android.net.Uri fileUri, String noteName) {
        isLoading.postValue(true);
        uploadStep.postValue("上传中...");
        uploadProgress.postValue(10);
        try {
            String mimeType = getApplication().getContentResolver().getType(fileUri);
            if (mimeType == null) mimeType = "application/octet-stream";

            String extension = getExtensionFromMime(mimeType);

            String fileName = (noteName != null ? noteName : "upload") + "_" + System.currentTimeMillis() + extension;

            InputStream is = getApplication().getContentResolver().openInputStream(fileUri);
            if (is == null) {
                error.postValue("无法读取文件");
                isLoading.postValue(false);
                uploadStep.postValue("");
                uploadProgress.postValue(0);
                return;
            }

            File tempFile = new File(getApplication().getCacheDir(), fileName);
            FileOutputStream fos = new FileOutputStream(tempFile);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            fos.close();
            is.close();

            uploadStep.postValue("分析中...");
            uploadProgress.postValue(40);

            MediaType mediaType = MediaType.parse(mimeType);
            RequestBody requestFile = RequestBody.create(tempFile, mediaType);
            MultipartBody.Part part = MultipartBody.Part.createFormData("file", fileName, requestFile);
            RequestBody titlePart = RequestBody.create(noteName != null ? noteName : "", MediaType.parse("text/plain"));

            repository.uploadNote(part, titlePart, new KnowledgeRepository.ResultCallback<UploadResult>() {
                @Override
                public void onSuccess(UploadResult data) {
                    uploadStep.postValue("生成中...");
                    uploadProgress.postValue(80);
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        uploadStep.postValue("完成");
                        uploadProgress.postValue(100);
                        isLoading.postValue(false);
                        uploadResult.postValue(data);
                        listNotes("", 0, 20);
                    }, 600);
                }

                @Override
                public void onError(int code, String message) {
                    isLoading.postValue(false);
                    uploadStep.postValue("");
                    uploadProgress.postValue(0);
                    error.postValue(message);
                }

                @Override
                public void onException(Exception e) {
                    isLoading.postValue(false);
                    uploadStep.postValue("");
                    uploadProgress.postValue(0);
                    error.postValue(e.getMessage());
                }
            });
        } catch (Exception e) {
            isLoading.postValue(false);
            uploadStep.postValue("");
            uploadProgress.postValue(0);
            error.postValue(e.getMessage());
        }
    }

    private String getExtensionFromMime(String mimeType) {
        if (mimeType == null) return "";
        switch (mimeType) {
            case "image/jpeg": return ".jpg";
            case "image/png":  return ".png";
            case "image/webp": return ".webp";
            case "application/pdf": return ".pdf";
            case "text/plain": return ".txt";
            case "text/markdown": return ".md";
            default:
                if (mimeType.startsWith("image/")) return ".img";
                return "";
        }
    }

    public void deleteNote(String noteId) {
        isLoading.postValue(true);
        repository.deleteNote(noteId, new KnowledgeRepository.ResultCallback<Boolean>() {
            @Override public void onSuccess(Boolean data) {
                isLoading.postValue(false);
                listNotes("", 0, 20);
            }
            @Override public void onError(int code, String msg) {
                isLoading.postValue(false);
                error.postValue("删除失败: " + msg);
            }
            @Override public void onException(Exception e) {
                isLoading.postValue(false);
                error.postValue(e.getMessage());
            }
        });
    }
}
