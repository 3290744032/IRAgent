package com.suiyuan.iragent_app.data.repository;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.suiyuan.iragent_app.data.local.PreferencesManager;
import com.suiyuan.iragent_app.data.local.UserInfoDao;
import com.suiyuan.iragent_app.data.model.*;
import com.suiyuan.iragent_app.data.remote.ApiService;
import com.suiyuan.iragent_app.data.remote.NetworkClient;

import java.io.InputStream;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    private final ApiService apiService;
    private final PreferencesManager preferencesManager;
    private final UserInfoDao userInfoDao;

    public AuthRepository(ApiService apiService, PreferencesManager preferencesManager, UserInfoDao userInfoDao) {
        this.apiService = apiService;
        this.preferencesManager = preferencesManager;
        this.userInfoDao = userInfoDao;
    }

    public interface ResultCallback<T> {
        void onSuccess(T data);
        void onError(int code, String message);
        void onException(Exception e);
    }

    private <T, R> Callback<ApiResponse<T>> createCallback(ResultCallback<R> callback, DataExtractor<T, R> extractor) {
        return new Callback<ApiResponse<T>>() {
            @Override
            public void onResponse(Call<ApiResponse<T>> call, Response<ApiResponse<T>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<T> body = response.body();
                    if (body.isSuccess()) {
                        R data = extractor.extract(body);
                        if (data != null) {
                            callback.onSuccess(data);
                        } else {
                            callback.onError(body.getCode(), body.getMessage());
                        }
                    } else {
                        callback.onError(body.getCode(), body.getMessage());
                    }
                } else {
                    callback.onError(response.code(), response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<T>> call, Throwable t) {
                callback.onException(new Exception(t));
            }
        };
    }

    private interface DataExtractor<T, R> {
        R extract(ApiResponse<T> response);
    }

    public void getVerifiCodeImage(ResultCallback<VerifiCodeResult> callback) {
        apiService.getVerifiCodeImage().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String uuid = response.headers().get("X-Verification-UUID");
                        InputStream inputStream = response.body().byteStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        preferencesManager.saveVerificationUuid(uuid);
                        callback.onSuccess(new VerifiCodeResult(bitmap, uuid));
                    } catch (Exception e) {
                        callback.onException(e);
                    }
                } else {
                    callback.onError(response.code(), response.message());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callback.onException(new Exception(t));
            }
        });
    }

    public void login(String account, String password, String verifiCode, ResultCallback<String> callback) {
        String uuid = preferencesManager.getVerificationUuid();
        LoginRequest request = new LoginRequest(account, password, verifiCode, uuid);
        apiService.login(request).enqueue(createCallback(callback, response -> {
            if (response.getData() == null) return null;
            String token = response.getData().getToken();
            android.util.Log.d("AuthRepository", "登录成功，收到token: " + token);
            preferencesManager.saveToken(token);
            NetworkClient.setToken(token);
            return token;
        }));
    }

    public void register(String account, String password, String email, String telphone, String verifiCode, ResultCallback<Void> callback) {
        String uuid = preferencesManager.getVerificationUuid();
        RegisterRequest request = new RegisterRequest(account, password, email, telphone, verifiCode, uuid);
        apiService.register(request).enqueue(createCallback(callback, response -> null));
    }

    public void logout() {
        preferencesManager.clearAll();
        if (userInfoDao != null) {
            userInfoDao.deleteUserInfo();
        }
        NetworkClient.setToken(null);
    }

    public boolean isLoggedIn() {
        return preferencesManager.isLoggedIn();
    }

    public String getToken() {
        return preferencesManager.getToken();
    }

    public static class VerifiCodeResult {
        public final Bitmap bitmap;
        public final String uuid;

        public VerifiCodeResult(Bitmap bitmap, String uuid) {
            this.bitmap = bitmap;
            this.uuid = uuid;
        }
    }
}