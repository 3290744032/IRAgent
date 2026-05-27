package com.suiyuan.iragent_app.ui.screens.profile;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.suiyuan.iragent_app.data.model.v3.DashboardOverview;
import com.suiyuan.iragent_app.data.model.v3.MasteryRadarData;
import com.suiyuan.iragent_app.data.model.v3.TaskItem;
import com.suiyuan.iragent_app.data.model.v3.WeeklyReport;
import com.suiyuan.iragent_app.data.remote.v3.ApiServiceV3;
import com.suiyuan.iragent_app.data.remote.v3.NetworkClientV3;
import com.suiyuan.iragent_app.data.repository.v3.DashboardRepository;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DashboardViewModel extends AndroidViewModel {

    private final DashboardRepository repository;
    private static final int TOTAL_CALLS = 4;

    private final MutableLiveData<DashboardOverview> overview = new MutableLiveData<>();
    private final MutableLiveData<MasteryRadarData> radarData = new MutableLiveData<>();
    private final MutableLiveData<List<TaskItem>> todayTasks = new MutableLiveData<>();
    private final MutableLiveData<WeeklyReport> weeklyReport = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        ApiServiceV3 apiService = NetworkClientV3.getApiService();
        this.repository = new DashboardRepository(apiService);
    }

    public MutableLiveData<DashboardOverview> getOverview() { return overview; }
    public MutableLiveData<MasteryRadarData> getRadarData() { return radarData; }
    public MutableLiveData<List<TaskItem>> getTodayTasks() { return todayTasks; }
    public MutableLiveData<WeeklyReport> getWeeklyReport() { return weeklyReport; }
    public MutableLiveData<String> getError() { return error; }
    public MutableLiveData<Boolean> getIsLoading() { return isLoading; }

    public void loadAllDashboard() {
        isLoading.postValue(true);
        AtomicInteger completed = new AtomicInteger(0);

        Runnable checkDone = () -> {
            if (completed.incrementAndGet() >= TOTAL_CALLS) {
                isLoading.postValue(false);
            }
        };

        repository.getOverview(new DashboardRepository.ResultCallback<DashboardOverview>() {
            @Override
            public void onSuccess(DashboardOverview data) { overview.postValue(data); checkDone.run(); }
            @Override
            public void onError(int code, String message) { error.postValue(message); checkDone.run(); }
            @Override
            public void onException(Exception e) { error.postValue(e.getMessage()); checkDone.run(); }
        });

        repository.getMasteryRadar(new DashboardRepository.ResultCallback<MasteryRadarData>() {
            @Override
            public void onSuccess(MasteryRadarData data) { radarData.postValue(data); checkDone.run(); }
            @Override
            public void onError(int code, String message) { error.postValue(message); checkDone.run(); }
            @Override
            public void onException(Exception e) { error.postValue(e.getMessage()); checkDone.run(); }
        });

        repository.getTodayTasks(new DashboardRepository.ResultCallback<List<TaskItem>>() {
            @Override
            public void onSuccess(List<TaskItem> data) { todayTasks.postValue(data); checkDone.run(); }
            @Override
            public void onError(int code, String message) { error.postValue(message); checkDone.run(); }
            @Override
            public void onException(Exception e) { error.postValue(e.getMessage()); checkDone.run(); }
        });

        repository.getWeeklyReport(new DashboardRepository.ResultCallback<WeeklyReport>() {
            @Override
            public void onSuccess(WeeklyReport data) { weeklyReport.postValue(data); checkDone.run(); }
            @Override
            public void onError(int code, String message) { checkDone.run(); }
            @Override
            public void onException(Exception e) { checkDone.run(); }
        });
    }
}
