package com.suiyuan.iragent_app.ui.screens.profile;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.suiyuan.iragent_app.data.local.PreferencesManager;
import com.suiyuan.iragent_app.data.remote.ApiService;
import com.suiyuan.iragent_app.data.remote.NetworkClient;
import com.suiyuan.iragent_app.data.repository.AuthRepository;

public class ProfileViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        ApiService apiService = NetworkClient.getApiService();
        PreferencesManager preferencesManager = new PreferencesManager(application.getApplicationContext());
        authRepository = new AuthRepository(apiService, preferencesManager, null);
    }

    public void logout() {
        authRepository.logout();
    }
}
