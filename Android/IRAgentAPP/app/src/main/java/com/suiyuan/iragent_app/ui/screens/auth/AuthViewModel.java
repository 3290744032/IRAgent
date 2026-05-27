package com.suiyuan.iragent_app.ui.screens.auth;

import android.app.Application;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.suiyuan.iragent_app.data.local.PreferencesManager;
import com.suiyuan.iragent_app.data.remote.ApiService;
import com.suiyuan.iragent_app.data.remote.NetworkClient;
import com.suiyuan.iragent_app.data.repository.AuthRepository;

public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;
    private final MutableLiveData<Bitmap> verifiCodeLiveData = new MutableLiveData<>();
    private final MutableLiveData<AuthResult> authResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        ApiService apiService = NetworkClient.getApiService();
        PreferencesManager preferencesManager = new PreferencesManager(application.getApplicationContext());
        authRepository = new AuthRepository(apiService, preferencesManager, null);
    }

    public LiveData<Bitmap> getVerifiCodeLiveData() {
        return verifiCodeLiveData;
    }

    public LiveData<AuthResult> getAuthResult() {
        return authResult;
    }

    public LiveData<Boolean> getLoadingLiveData() {
        return loadingLiveData;
    }

    public void loadVerifiCode() {
        authRepository.getVerifiCodeImage(new AuthRepository.ResultCallback<AuthRepository.VerifiCodeResult>() {
            @Override
            public void onSuccess(AuthRepository.VerifiCodeResult data) {
                verifiCodeLiveData.postValue(data.bitmap);
            }

            @Override
            public void onError(int code, String message) {
                authResult.postValue(new AuthResult(false, "获取验证码失败"));
            }

            @Override
            public void onException(Exception e) {
                authResult.postValue(new AuthResult(false, "获取验证码异常: " + e.getMessage()));
            }
        });
    }

    public void login(String account, String password, String verifiCode) {
        loadingLiveData.postValue(true);
        authRepository.login(account, password, verifiCode, new AuthRepository.ResultCallback<String>() {
            @Override
            public void onSuccess(String data) {
                loadingLiveData.postValue(false);
                authResult.postValue(new AuthResult(true, "登录成功"));
            }

            @Override
            public void onError(int code, String message) {
                loadingLiveData.postValue(false);
                authResult.postValue(new AuthResult(false, message));
            }

            @Override
            public void onException(Exception e) {
                loadingLiveData.postValue(false);
                authResult.postValue(new AuthResult(false, "登录异常: " + e.getMessage()));
            }
        });
    }

    public void register(String account, String password, String email, String telphone, String verifiCode) {
        loadingLiveData.postValue(true);
        authRepository.register(account, password, email, telphone, verifiCode, new AuthRepository.ResultCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                loadingLiveData.postValue(false);
                authResult.postValue(new AuthResult(true, "注册成功"));
            }

            @Override
            public void onError(int code, String message) {
                loadingLiveData.postValue(false);
                authResult.postValue(new AuthResult(false, message));
            }

            @Override
            public void onException(Exception e) {
                loadingLiveData.postValue(false);
                authResult.postValue(new AuthResult(false, "注册异常: " + e.getMessage()));
            }
        });
    }

    public static class AuthResult {
        private final boolean success;
        private final String message;

        public AuthResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
