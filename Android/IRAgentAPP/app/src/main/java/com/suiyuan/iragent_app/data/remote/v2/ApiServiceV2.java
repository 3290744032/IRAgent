package com.suiyuan.iragent_app.data.remote.v2;

import com.suiyuan.iragent_app.data.model.ApiResponse;
import com.suiyuan.iragent_app.data.model.GenerateTimelineRequest;
import com.suiyuan.iragent_app.data.model.v2.*;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiServiceV2 {

    @POST("sessions")
    Call<ApiResponse<SessionResponse>> createSession(@Body CreateSessionRequest request);

    @GET("sessions/{sessionId}")
    Call<ApiResponse<SessionResponse>> getSessionDetail(@Path("sessionId") String sessionId);

    @GET("sessions/{sessionId}/teach")
    @Headers("Accept: text/event-stream")
    @Streaming
    Call<ResponseBody> getTeachStream(
            @Path("sessionId") String sessionId,
            @Query("mode") String mode);

    @POST("sessions/{sessionId}/answer")
    Call<ApiResponse<AnswerResponse>> submitAnswer(
            @Path("sessionId") String sessionId,
            @Body AnswerRequest request);

    @GET("sessions/{sessionId}/summary/stream")
    @Headers("Accept: text/event-stream")
    @Streaming
    Call<ResponseBody> getSummaryStream(@Path("sessionId") String sessionId);

    @GET("sessions/history")
    Call<ApiResponse<SessionHistoryResponse>> getSessionHistory(
            @Query("page") int page,
            @Query("size") int size);

    @DELETE("sessions/{sessionId}")
    Call<ApiResponse<DeleteSessionResponse>> deleteSession(@Path("sessionId") String sessionId);

    @POST("/api/timeline/generate")
    @Headers("Content-Type: application/json")
    Call<ResponseBody> generateTimeline(@Body GenerateTimelineRequest request);
}
