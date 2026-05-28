package com.suiyuan.iragent_app.data.remote.v3;

import com.suiyuan.iragent_app.data.model.ApiResponse;
import com.suiyuan.iragent_app.data.model.v3.*;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiServiceV3 {

    // ===== 知识库 =====
    @GET("kb/notes")
    Call<ApiResponse<List<NoteItem>>> listNotes(
            @Query("subject") String subject,
            @Query("page") int page,
            @Query("size") int size);

    @GET("kb/notes/{id}")
    Call<ApiResponse<NoteDetail>> getNoteDetail(@Path("id") String id);

    @PUT("kb/notes/{id}")
    Call<ApiResponse<Map<String, Object>>> updateNote(@Path("id") String id, @Body Map<String, String> body);

    @POST("kb/notes/{id}/optimize")
    @retrofit2.http.Headers("Content-Type: application/json")
    Call<ApiResponse<Map<String, Object>>> optimizeNote(@Path("id") String id, @Body Map<String, String> body);

    @DELETE("kb/notes/{id}")
    Call<ApiResponse<Map<String, Object>>> deleteNote(@Path("id") String id);

    @Multipart
    @POST("kb/upload")
    Call<ApiResponse<UploadResult>> uploadNote(@Part MultipartBody.Part file, @Part("title") okhttp3.RequestBody title);

    @POST("kb/search")
    Call<ApiResponse<List<NoteFragment>>> searchNotes(@Body SearchRequest body);

    @GET("kb/graph-data")
    Call<ApiResponse<Map<String, Object>>> getGraphData();

    // ===== 错题本 =====
    @GET("errors/list")
    Call<ApiResponse<List<ErrorItem>>> listErrors(
            @Query("subject") String subject,
            @Query("errorType") String errorType,
            @Query("page") int page,
            @Query("size") int size);

    @GET("errors/{id}")
    Call<ApiResponse<ErrorDetail>> getErrorDetail(@Path("id") String id);

    @GET("errors/review-queue")
    Call<ApiResponse<List<ReviewItem>>> getReviewQueue();

    @PUT("errors/{id}/mark-mastered")
    Call<ApiResponse<Map<String, Object>>> markMastered(@Path("id") String id);

    @PUT("errors/{id}/unmark-mastered")
    Call<ApiResponse<Map<String, Object>>> unmarkMastered(@Path("id") String id);

    @POST("errors/{id}/similar")
    Call<ApiResponse<List<SimilarQuestion>>> getSimilarQuestions(@Path("id") String id);

    // ===== 真题库 =====
    @GET("exam-archive")
    Call<ApiResponse<List<ExamQuestion>>> listExamArchive(
            @Query("subject") String subject,
            @Query("year") Integer year,
            @Query("examType") String examType,
            @Query("knowledgePoint") String knowledgePoint,
            @Query("difficulty") Integer difficulty,
            @Query("page") int page,
            @Query("size") int size);

    @GET("exam-archive/filters")
    Call<ApiResponse<ExamFilterData>> getExamFilters();

    @POST("exam-archive/simulate")
    Call<ApiResponse<List<ExamQuestion>>> simulateExamArchive(@Body SimulateExamRequest body);

    @POST("exam-archive/feedback")
    Call<ApiResponse<Map<String, Object>>> submitQuestionFeedback(@Body QuestionFeedbackRequest body);

    // ===== 每日一练 =====
    @GET("daily-practice")
    Call<ApiResponse<DailyPracticeSession>> getDailyPractice(
            @Query("subject") String subject,
            @Query("count") int count,
            @Query("knowledgePoints") String knowledgePoints);

    @POST("daily-practice/submit")
    Call<ApiResponse<SubmitAnswerResult>> submitDailyPractice(@Body SubmitAnswerRequest body);

    // ===== 智能组卷 =====
    @POST("paper/smart")
    Call<ApiResponse<SmartPaper>> generateSmartPaper(@Body SmartPaperRequest body);

    @POST("paper/submit")
    Call<ApiResponse<SubmitAnswerResult>> submitPaper(@Body SubmitAnswerRequest body);

    // ===== 仪表盘 =====
    @GET("dashboard/overview")
    Call<ApiResponse<DashboardOverview>> getOverview();

    @GET("dashboard/mastery-radar")
    Call<ApiResponse<MasteryRadarData>> getMasteryRadar();

    @GET("dashboard/today-tasks")
    Call<ApiResponse<List<TaskItem>>> getTodayTasks();

    @GET("dashboard/weekly-report")
    Call<ApiResponse<WeeklyReport>> getWeeklyReport();
}
