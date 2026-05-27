package com.suiyuan.iragent_app.data.remote.v3;

import com.suiyuan.iragent_app.BuildConfig;
import com.suiyuan.iragent_app.data.remote.ApiService;
import com.suiyuan.iragent_app.data.remote.NetworkClient;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NetworkClientV3 {

    private static final String BASE_URL = com.suiyuan.iragent_app.BuildConfig.API_HOST + "/api/v3/";
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

    private static final HttpLoggingInterceptor streamLoggingInterceptor = new HttpLoggingInterceptor()
            .setLevel(HttpLoggingInterceptor.Level.HEADERS);

    private static final OkHttpClient streamOkHttpClient = new OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(streamLoggingInterceptor)
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

    private static final ApiServiceV3 apiService = retrofit.create(ApiServiceV3.class);
    private static final ApiServiceV3 streamApiService = streamRetrofit.create(ApiServiceV3.class);

    // 会话管理 API（V3 后端没有会话端点，使用 V1 /api/ 路径）
    private static final String CONVERSATION_BASE_URL = BuildConfig.API_HOST + "/api/";
    private static final Retrofit conversationRetrofit = new Retrofit.Builder()
            .baseUrl(CONVERSATION_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
    private static final ApiService conversationApiService = conversationRetrofit.create(ApiService.class);

    public static ApiServiceV3 getApiService() {
        return apiService;
    }

    public static ApiServiceV3 getStreamApiService() {
        return streamApiService;
    }

    public static ApiService getConversationApiService() {
        return conversationApiService;
    }

    public static OkHttpClient getStreamOkHttpClient() {
        return streamOkHttpClient;
    }

    public static String getBaseUrl() {
        return BASE_URL;
    }
}
