package com.suiyuan.iragent_app.ui.screens.practice;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.suiyuan.iragent_app.data.model.v3.GradingReport;
import com.suiyuan.iragent_app.data.repository.v3.PracticeRepository;

public class PracticeHubViewModel extends AndroidViewModel {

    private final PracticeRepository repository;

    private final MutableLiveData<String> gradingStep = new MutableLiveData<>();
    private final MutableLiveData<Integer> gradingProgress = new MutableLiveData<>(0);
    private final MutableLiveData<GradingReport> gradingReport = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isGrading = new MutableLiveData<>(false);

    public PracticeHubViewModel(@NonNull Application application) {
        super(application);
        this.repository = new PracticeRepository();
    }

    public MutableLiveData<String> getGradingStep() { return gradingStep; }
    public MutableLiveData<Integer> getGradingProgress() { return gradingProgress; }
    public MutableLiveData<GradingReport> getGradingReport() { return gradingReport; }
    public MutableLiveData<String> getError() { return error; }
    public MutableLiveData<Boolean> getIsGrading() { return isGrading; }

    public void submitGrading(String content, String subjectType, int maxScore) {
        isGrading.postValue(true);
        gradingProgress.postValue(0);

        repository.submitGrading(content, subjectType, maxScore, new PracticeRepository.GradingCallback() {
            @Override
            public void onStart() {
                gradingStep.postValue("开始批改...");
            }

            @Override
            public void onStep(String step, String text, int current, int total) {
                String stepText;
                switch (step) {
                    case "ocr": stepText = "OCR 识别手写文字..."; break;
                    case "extract": stepText = "提取 " + (text.isEmpty() ? "" : text) + " 道题目"; break;
                    case "grade": stepText = "批改 " + current + "/" + total; break;
                    case "diagnose": stepText = "诊断错题 " + current; break;
                    case "complete": stepText = "批改完成"; break;
                    default: stepText = step;
                }
                gradingStep.postValue(stepText);
                if (total > 0) gradingProgress.postValue(current * 100 / total);
            }

            @Override
            public void onComplete(GradingReport report) {
                isGrading.postValue(false);
                gradingReport.postValue(report);
            }

            @Override
            public void onError(int code, String message) {
                isGrading.postValue(false);
                error.postValue(message);
            }

            @Override
            public void onException(Exception e) {
                isGrading.postValue(false);
                error.postValue(e.getMessage());
            }
        });
    }
}
