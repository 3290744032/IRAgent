package com.suiyuan.iragent_app.ui.screens.practice;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.suiyuan.iragent_app.data.model.v3.*;
import com.suiyuan.iragent_app.data.repository.v3.PracticeV2Repository;
import java.util.ArrayList;
import java.util.List;

public class SmartPaperViewModel extends AndroidViewModel {

    private final PracticeV2Repository repository;
    private final MutableLiveData<SmartPaper> paper = new MutableLiveData<>();
    private final MutableLiveData<SubmitAnswerResult> result = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Integer> questionIndex = new MutableLiveData<>(0);
    private final MutableLiveData<String> selectedOption = new MutableLiveData<>("");

    private final List<SubmitAnswerRequest.AnswerEntry> answers = new ArrayList<>();

    public SmartPaperViewModel(@NonNull Application application) {
        super(application);
        this.repository = new PracticeV2Repository();
    }

    public MutableLiveData<SmartPaper> getPaper() { return paper; }
    public MutableLiveData<SubmitAnswerResult> getResult() { return result; }
    public MutableLiveData<Boolean> getIsLoading() { return isLoading; }
    public MutableLiveData<String> getError() { return error; }
    public MutableLiveData<Integer> getQuestionIndex() { return questionIndex; }
    public MutableLiveData<String> getSelectedOption() { return selectedOption; }

    public void generatePaper(SmartPaperRequest req) {
        isLoading.postValue(true);
        answers.clear();
        questionIndex.postValue(0);
        repository.generateSmartPaper(req,
                new PracticeV2Repository.ResultCallback<SmartPaper>() {
                    @Override public void onSuccess(SmartPaper data) { paper.postValue(data); isLoading.postValue(false); }
                    @Override public void onError(int code, String message) { error.postValue(message); isLoading.postValue(false); }
                    @Override public void onException(Exception e) { error.postValue(e.getMessage()); isLoading.postValue(false); }
                });
    }

    public void selectOption(String option) {
        selectedOption.postValue(option);
    }

    public void nextQuestion(String questionId) {
        String sel = selectedOption.getValue();
        if (sel != null && !sel.isEmpty()) {
            answers.add(new SubmitAnswerRequest.AnswerEntry(questionId, sel, 30));
        }
        selectedOption.postValue("");
        SmartPaper p = paper.getValue();
        if (p != null && questionIndex.getValue() != null
                && questionIndex.getValue() < p.getQuestions().size() - 1) {
            questionIndex.postValue(questionIndex.getValue() + 1);
        }
    }

    public boolean isLastQuestion() {
        SmartPaper p = paper.getValue();
        return p == null || questionIndex.getValue() == null
                || questionIndex.getValue() >= p.getQuestions().size() - 1;
    }

    public void submitAll(String paperId) {
        isLoading.postValue(true);
        SubmitAnswerRequest req = new SubmitAnswerRequest(paperId, "smart_paper", answers);
        repository.submitPaper(req, new PracticeV2Repository.ResultCallback<SubmitAnswerResult>() {
            @Override public void onSuccess(SubmitAnswerResult data) { result.postValue(data); isLoading.postValue(false); }
            @Override public void onError(int code, String message) { error.postValue(message); isLoading.postValue(false); }
            @Override public void onException(Exception e) { error.postValue(e.getMessage()); isLoading.postValue(false); }
        });
    }
}
