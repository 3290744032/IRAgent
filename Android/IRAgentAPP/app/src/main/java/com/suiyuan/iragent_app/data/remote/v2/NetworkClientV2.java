package com.suiyuan.iragent_app.data.remote.v2;

import com.suiyuan.iragent_app.data.remote.NetworkClient;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NetworkClientV2 {

    private static final String BASE_URL = com.suiyuan.iragent_app.BuildConfig.API_HOST + "/api/v2/";
    private static final long TIMEOUT_SECONDS = 180L;
    private static final long STREAM_TIMEOUT_SECONDS = 300L;

    private static final Interceptor authInterceptor = chain -> {
        String token = NetworkClient.getToken();
        Request originalRequest = chain.request();
        Request.Builder builder = originalRequest.newBuilder();
        if (token != null && !token.isEmpty()) {
            builder.header("token", token);
        }
        return chain.proceed(builder.build());
    };

    private static final HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor()
            .setLevel(HttpLoggingInterceptor.Level.BODY);

    // 401 响应拦截器 — token过期自动跳转登录
    private static final okhttp3.Interceptor unauthorizedInterceptor = chain -> {
        okhttp3.Response response = chain.proceed(chain.request());
        if (response.code() == 401) {
            com.suiyuan.iragent_app.IRAgentApplication app =
                    com.suiyuan.iragent_app.IRAgentApplication.getInstance();
            if (app != null) {
                app.onUnauthorized();
            }
        }
        return response;
    };

    private static final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(unauthorizedInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build();

    private static final OkHttpClient streamOkHttpClient = new OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(unauthorizedInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(STREAM_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addNetworkInterceptor(chain -> {
                Request request = chain.request().newBuilder()
                        .header("Accept-Encoding", "identity")
                        .header("Connection", "keep-alive")
                        .header("Accept", "text/event-stream")
                        .build();
                return chain.proceed(request);
            })
            .build();

    private static final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    private static final Retrofit streamRetrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(streamOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    private static final ApiServiceV2 apiService = retrofit.create(ApiServiceV2.class);
    private static final ApiServiceV2 streamApiService = streamRetrofit.create(ApiServiceV2.class);

    public static ApiServiceV2 getApiService() {
        return apiService;
    }

    public static ApiServiceV2 getStreamApiService() {
        return streamApiService;
    }
}
