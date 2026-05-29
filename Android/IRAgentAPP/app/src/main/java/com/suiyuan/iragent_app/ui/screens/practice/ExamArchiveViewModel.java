package com.suiyuan.iragent_app.ui.screens.practice;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.suiyuan.iragent_app.data.model.v3.ExamFilterData;
import com.suiyuan.iragent_app.data.model.v3.ExamQuestion;
import com.suiyuan.iragent_app.data.model.v3.SimulateExamRequest;
import com.suiyuan.iragent_app.data.repository.v3.PracticeV2Repository;
import java.util.List;
import java.util.Map;

public class ExamArchiveViewModel extends AndroidViewModel {

    private final PracticeV2Repository repository;
    private final MutableLiveData<List<ExamQuestion>> questions = new MutableLiveData<>();
    private final MutableLiveData<ExamFilterData> filters = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private String currentSubject = "";
    private Integer currentYear = null;
    private String currentExamType = "";

    public ExamArchiveViewModel(@NonNull Application application) {
        super(application);
        this.repository = new PracticeV2Repository();
    }

    public LiveData<List<ExamQuestion>> getQuestions() { return questions; }
    public LiveData<ExamFilterData> getFilters() { return filters; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getError() { return error; }

    public void loadFilters() {
        repository.getExamFilters(new PracticeV2Repository.ResultCallback<ExamFilterData>() {
            @Override public void onSuccess(ExamFilterData data) { filters.postValue(data); }
            @Override public void onError(int code, String message) { error.postValue(message); }
            @Override public void onException(Exception e) { error.postValue(e.getMessage()); }
        });
    }

    public void search(String subject, Integer year, String examType,
                       String knowledgePoint, Integer difficulty, int page, int size) {
        isLoading.postValue(true);
        this.currentSubject = subject;
        this.currentYear = year;
        this.currentExamType = examType;
        repository.listExamArchive(subject, year, examType, knowledgePoint, difficulty, page, size,
                new PracticeV2Repository.ResultCallback<List<ExamQuestion>>() {
                    @Override public void onSuccess(List<ExamQuestion> data) { questions.postValue(data); isLoading.postValue(false); }
                    @Override public void onError(int code, String message) { error.postValue(message); isLoading.postValue(false); }
                    @Override public void onException(Exception e) { error.postValue(e.getMessage()); isLoading.postValue(false); }
                });
    }

    public void simulate(String subject, String examType, int count) {
        isLoading.postValue(true);
        repository.simulateExamArchive(new SimulateExamRequest(subject, examType, count),
                new PracticeV2Repository.ResultCallback<List<ExamQuestion>>() {
                    @Override public void onSuccess(List<ExamQuestion> data) { questions.postValue(data); isLoading.postValue(false); }
                    @Override public void onError(int code, String message) { error.postValue(message); isLoading.postValue(false); }
                    @Override public void onException(Exception e) { error.postValue(e.getMessage()); isLoading.postValue(false); }
                });
    }

    public void submitFeedback(String questionId, String feedbackType, String comment) {
        repository.submitQuestionFeedback(
                new com.suiyuan.iragent_app.data.model.v3.QuestionFeedbackRequest(questionId, feedbackType, comment),
                new PracticeV2Repository.ResultCallback<Map<String, Object>>() {
                    @Override public void onSuccess(Map<String, Object> data) { error.postValue("反馈已提交，感谢你的帮助！"); }
                    @Override public void onError(int code, String message) { error.postValue("反馈提交失败: " + message); }
                    @Override public void onException(Exception e) { error.postValue("反馈提交异常: " + e.getMessage()); }
                });
    }

    public void uploadPaper(android.net.Uri uri) {
        isLoading.postValue(true);
        try {
            java.io.InputStream is = getApplication().getContentResolver().openInputStream(uri);
            byte[] bytes = new byte[is.available()];
            is.read(bytes);
            is.close();
            okhttp3.MediaType mt = okhttp3.MediaType.parse("image/*");
            okhttp3.RequestBody body = okhttp3.RequestBody.create(mt, bytes);
            okhttp3.MultipartBody.Part part = okhttp3.MultipartBody.Part.createFormData("file", "paper.jpg", body);
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder().connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS).readTimeout(60, java.util.concurrent.TimeUnit.SECONDS).build();
            okhttp3.Request req = new okhttp3.Request.Builder().url("http://192.168.123.44:8080/api/v3/exam-archive/upload").addHeader("token", com.suiyuan.iragent_app.data.remote.NetworkClient.getToken()).post(new okhttp3.MultipartBody.Builder().setType(okhttp3.MultipartBody.FORM).addFormDataPart("file", "paper.jpg", body).addFormDataPart("subject", com.suiyuan.iragent_app.config.SubjectConfig.DEFAULT_SUBJECT).build()).build();
            client.newCall(req).enqueue(new okhttp3.Callback() {
                @Override public void onResponse(okhttp3.Call c, okhttp3.Response r) { isLoading.postValue(false); search(null, null, null, null, null, 0, 20); }
                @Override public void onFailure(okhttp3.Call c, java.io.IOException e) { isLoading.postValue(false); error.postValue("上传失败"); }
            });
        } catch (Exception e) { isLoading.postValue(false); error.postValue(e.getMessage()); }
    }
}