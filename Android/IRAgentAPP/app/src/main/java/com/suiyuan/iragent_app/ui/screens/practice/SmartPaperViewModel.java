package com.suiyuan.iragent_app.ui.screens.practice;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.suiyuan.iragent_app.data.model.v3.*;
import com.suiyuan.iragent_app.data.repository.v3.PracticeV2Repository;
import com.suiyuan.iragent_app.data.repository.v3.SmartPaperStreamRepository;

import java.util.ArrayList;
import java.util.List;

public class SmartPaperViewModel extends AndroidViewModel {

    private final PracticeV2Repository repository;
    private final SmartPaperStreamRepository streamRepository;
    private final MutableLiveData<SmartPaper> paper = new MutableLiveData<>();
    private final MutableLiveData<SubmitAnswerResult> result = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Integer> questionIndex = new MutableLiveData<>(0);
    private final MutableLiveData<String> selectedOption = new MutableLiveData<>("");

    // Streaming state
    private final StringBuilder streamBuffer = new StringBuilder();
    private final StringBuilder paperBodyBuffer = new StringBuilder();
    private final MutableLiveData<String> streamContent = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> isStreaming = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> pdfVisible = new MutableLiveData<>(false);
    private final MutableLiveData<String> streamError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> answerKeyVisible = new MutableLiveData<>(false);
    private boolean separatorReached = false;
    private boolean completed = false;
    private String answerKeyContent;
    private String fullContent;
    private String paperBodyContent;
    private String paperId;
    private SmartPaper streamedPaper;
    private static final String ANSWER_SEPARATOR = "---ANSWER_BREAK---";
    private static final java.util.regex.Pattern JSON_BLOCK =
            java.util.regex.Pattern.compile("```json[\\s\\S]*?```", java.util.regex.Pattern.DOTALL);

    private final List<SubmitAnswerRequest.AnswerEntry> answers = new ArrayList<>();

    public SmartPaperViewModel(@NonNull Application application) {
        super(application);
        this.repository = new PracticeV2Repository();
        this.streamRepository = new SmartPaperStreamRepository();
    }

    public LiveData<SmartPaper> getPaper() { return paper; }
    public LiveData<SubmitAnswerResult> getResult() { return result; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getError() { return error; }
    public LiveData<Integer> getQuestionIndex() { return questionIndex; }
    public LiveData<String> getSelectedOption() { return selectedOption; }

    // Streaming LiveData
    public LiveData<String> getStreamContent() { return streamContent; }
    public LiveData<Boolean> getIsStreaming() { return isStreaming; }
    public LiveData<Boolean> getPdfVisible() { return pdfVisible; }
    public LiveData<String> getStreamError() { return streamError; }
    public LiveData<Boolean> getAnswerKeyVisible() { return answerKeyVisible; }

    public String getPaperId() { return paperId; }
    public SmartPaper getStreamedPaper() { return streamedPaper; }
    public boolean hasAnswerKey() { return answerKeyContent != null && !answerKeyContent.isEmpty(); }
    public String getFullContent() {
        if (fullContent == null) return null;
        return JSON_BLOCK.matcher(fullContent).replaceAll("").trim();
    }
    public String getAnswerKeyContent() {
        return answerKeyContent;
    }

    public String getPaperBodyContent() {
        if (paperBodyContent == null) return null;
        return JSON_BLOCK.matcher(paperBodyContent).replaceAll("").trim();
    }

    public void toggleAnswerKey() {
        boolean cur = Boolean.TRUE.equals(answerKeyVisible.getValue());
        answerKeyVisible.postValue(!cur);
    }

    /**
     * 流式生成试卷 — 发送自然语言 prompt
     */
    public void streamGeneratePaper(String prompt) {
        isStreaming.postValue(true);
        pdfVisible.postValue(false);
        answerKeyVisible.postValue(false);
        streamBuffer.setLength(0);
        paperBodyBuffer.setLength(0);
        streamContent.postValue("");
        streamError.postValue("");
        answerKeyContent = null;
        fullContent = null;
        paperBodyContent = null;
        separatorReached = false;
        completed = false;
        answers.clear();
        questionIndex.postValue(0);

        streamRepository.streamGeneratePaper(prompt, new SmartPaperStreamRepository.StreamCallback() {
            @Override
            public void onChunk(String content) {
                if (completed) return;
                streamBuffer.append(content);

                if (!separatorReached) {
                    String full = streamBuffer.toString();
                    int sepIdx = full.indexOf(ANSWER_SEPARATOR);
                    if (sepIdx >= 0) {
                        separatorReached = true;
                        String body = full.substring(0, sepIdx).trim();
                        String aKey = full.substring(sepIdx + ANSWER_SEPARATOR.length()).trim();
                        paperBodyBuffer.setLength(0);
                        paperBodyBuffer.append(body);
                        paperBodyContent = body;
                        fullContent = body + "\n\n" + aKey;
                        streamContent.postValue(body);
                        answerKeyContent = aKey;
                    } else {
                        paperBodyBuffer.append(content);
                        streamContent.postValue(paperBodyBuffer.toString());
                    }
                } else {
                    answerKeyContent += content;
                    fullContent = paperBodyContent + "\n\n" + answerKeyContent;
                }
            }

            @Override
            public void onComplete(String pid, SmartPaper sp) {
                completed = true;
                if (!separatorReached) {
                    paperBodyContent = streamBuffer.toString();
                    fullContent = paperBodyContent;
                } else {
                    paperBodyContent = paperBodyBuffer.toString();
                    fullContent = paperBodyContent + "\n\n" + (answerKeyContent != null ? answerKeyContent : "");
                }

                paperId = pid;
                streamedPaper = sp;
                if (sp != null) {
                    paper.postValue(sp);
                }
                isStreaming.postValue(false);
                pdfVisible.postValue(true);
            }

            @Override
            public void onError(int code, String message) {
                completed = true;
                isStreaming.postValue(false);
                streamError.postValue("请求失败(" + code + "): " + message);
            }

            @Override
            public void onException(Exception e) {
                completed = true;
                isStreaming.postValue(false);
                streamError.postValue("网络异常: " + e.getMessage());
            }
        });
    }

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
