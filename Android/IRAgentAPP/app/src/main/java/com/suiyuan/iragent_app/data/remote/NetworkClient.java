package com.suiyuan.iragent_app.data.remote;

import com.suiyuan.iragent_app.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NetworkClient {

    private static final String BASE_URL = BuildConfig.API_HOST + "/api/";
    private static final long TIMEOUT_SECONDS = 60L;
    private static final long STREAM_TIMEOUT_SECONDS = 300L; // 5分钟，用于流式响应

    private static String token;

    public static void setToken(String token) {
        NetworkClient.token = token;
    }

    public static String getToken() {
        return token;
    }

    private static final Interceptor authInterceptor = chain -> {
        Request originalRequest = chain.request();
        Request.Builder builder = originalRequest.newBuilder();
        if (token != null && !token.isEmpty()) {
            builder.header("token", token);
        }
        return chain.proceed(builder.build());
    };

    private static final HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor()
            .setLevel(HttpLoggingInterceptor.Level.BODY);

    static {
        // 调试期间注释掉打码，以便查看实际发送的 token 值
        // loggingInterceptor.redactHeader("Authorization");
        // loggingInterceptor.redactHeader("token");
    }

    // 401 响应拦截器 — token过期自动跳转登录
    private static final Interceptor unauthorizedInterceptor = chain -> {
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

    // 普通请求的 OkHttpClient（带日志）
    private static final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(unauthorizedInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build();

    // 流式请求的 OkHttpClient（关键：不带日志拦截器，避免缓冲）
    private static final OkHttpClient streamOkHttpClient = new OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(unauthorizedInterceptor)
            // 重要：流式请求不添加日志拦截器，防止缓冲响应体
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(STREAM_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            // 关键：禁用透明压缩，防止缓冲导致假流式
            .addNetworkInterceptor(chain -> {
                Request request = chain.request().newBuilder()
                        .header("Accept-Encoding", "identity") // 禁用 gzip 压缩
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

    // 流式请求专用的 Retrofit（需要 Gson 转换器处理 @Body 参数）
    private static final Retrofit streamRetrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(streamOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    private static final ApiService apiService = retrofit.create(ApiService.class);
    private static final ApiService streamApiService = streamRetrofit.create(ApiService.class);

    public static ApiService getApiService() {
        return apiService;
    }

    public static ApiService getStreamApiService() {
        return streamApiService;
    }

    public static OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }
}
