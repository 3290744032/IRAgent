package com.suiyuan.iragent_app.data.remote;

import com.suiyuan.iragent_app.data.model.*;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    @GET("auth/getVerifiCodeImage")
    Call<ResponseBody> getVerifiCodeImage();

    @POST("auth/login")
    Call<ApiResponse<LoginResponse>> login(@Body LoginRequest request);

    @POST("auth/register")
    Call<ApiResponse<Void>> register(@Body RegisterRequest request);

    @Headers("Accept: text/event-stream")
    @POST("ai/solve/stream")
    @Streaming
    Call<ResponseBody> solveStream(@Body SolveRequest request);

    @GET("ai/chat/messages/{conversationId}")
    Call<ApiResponse<MessageListResponse>> getChatMessages(@Path("conversationId") String conversationId);

    @GET("conversations")
    Call<ApiResponse<java.util.List<Conversation>>> getConversations(
            @Query("page") Integer page,
            @Query("size") Integer size
    );

    @GET("conversations/all")
    Call<ApiResponse<java.util.List<Conversation>>> getAllConversations();

    @POST("conversations")
    Call<ApiResponse<ConversationListResponse>> createConversation(@Body CreateConversationRequest request);

    @GET("conversations/{conversationId}")
    Call<ApiResponse<ConversationListResponse>> getConversation(@Path("conversationId") String conversationId);

    @PUT("conversations/{conversationId}")
    Call<ApiResponse<ConversationListResponse>> updateConversation(
            @Path("conversationId") String conversationId,
            @Body UpdateConversationRequest request
    );

    @DELETE("conversations/{conversationId}")
    Call<ApiResponse<DeleteConversationResponse>> deleteConversation(@Path("conversationId") String conversationId);

    @GET("conversations/{conversationId}/messages")
    Call<ApiResponse<java.util.List<Message>>> getConversationMessages(@Path("conversationId") String conversationId);

    @POST("timeline/title")
    Call<ApiResponse<TimelineTitleResponse>> generateTimelineTitle(@Body GenerateTimelineRequest request);
}
