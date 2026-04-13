package com.estudy.app.api;

import com.estudy.app.model.request.AnswerRequest;
import com.estudy.app.model.request.CommentRequest;
import com.estudy.app.model.request.FlashCardImportRequest;
import com.estudy.app.model.request.FlashCardRequest;
import com.estudy.app.model.request.FlashCardSetRequest;
import com.estudy.app.model.request.LoginRequest;
import com.estudy.app.model.request.LogoutRequest;
import com.estudy.app.model.request.RegisterRequest;
import com.estudy.app.model.request.StartSessionRequest;
import com.estudy.app.model.response.ApiResponse;
import com.estudy.app.model.response.AuthResponse;
import com.estudy.app.model.response.CommentResponse;
import com.estudy.app.model.response.DayActivityResponse;
import com.estudy.app.model.response.FlashCardResponse;
import com.estudy.app.model.response.FlashCardSetDetailResponse;
import com.estudy.app.model.response.FlashCardSetResponse;
import com.estudy.app.model.response.ImportResultResponse;
import com.estudy.app.model.response.SessionResultResponse;
import com.estudy.app.model.response.SetProgressResponse;
import com.estudy.app.model.response.StartSessionResponse;
import com.estudy.app.model.response.StatSummaryResponse;
import com.estudy.app.model.response.StudyTodayResponse;
import com.estudy.app.model.response.SuggestResponse;
import com.estudy.app.model.response.UserResponse;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────
    @POST("auth/register")
    Call<ApiResponse<UserResponse>> register(@Body RegisterRequest request);

    @POST("auth/login")
    Call<ApiResponse<AuthResponse>> login(@Body LoginRequest request);

    @POST("auth/logout")
    Call<ApiResponse<Void>> logout(@Body LogoutRequest request);

    // ── FlashCard Set ─────────────────────────────────────────────────
    @GET("flashcard-sets/my")
    Call<ApiResponse<List<FlashCardSetResponse>>> getMyFlashCardSets();

    @GET("flashcard-sets/{id}")
    Call<ApiResponse<FlashCardSetDetailResponse>> getFlashCardSetDetail(@Path("id") String id);

    @POST("flashcard-sets")
    Call<ApiResponse<FlashCardSetResponse>> createFlashCardSet(@Body FlashCardSetRequest request);

    @PUT("flashcard-sets/{id}")
    Call<ApiResponse<FlashCardSetResponse>> updateFlashCardSet(
            @Path("id") String id, @Body FlashCardSetRequest request);

    @DELETE("flashcard-sets/{id}")
    Call<ApiResponse<Void>> deleteFlashCardSet(@Path("id") String id);

    // ── Comment ───────────────────────────────────────────────────────
    @POST("flashcard-sets/{flashCardSetId}/comments")
    Call<ApiResponse<CommentResponse>> addComment(
            @Path("flashCardSetId") String flashCardSetId,
            @Body CommentRequest request);

    @DELETE("flashcards/{id}")
    Call<ApiResponse<Void>> deleteFlashCard(@Path("id") String id);

    @DELETE("flashcard-sets/comments/{commentId}")
    Call<ApiResponse<Void>> deleteComment(@Path("commentId") String commentId);


    // ── FlashCards ──────────────────────────────────────────────────────────────

    @GET("flashcards")
    Call<ApiResponse<List<FlashCardResponse>>> getFlashCards(@Query("deck_id") String deckId);

    @POST("flashcard-sets/{setId}/flashcards")
    Call<ApiResponse<FlashCardResponse>> createFlashCard(
            @Path("setId") String setId, @Body FlashCardRequest request);

    @POST("flashcard-sets/{setId}/flashcards/import")
    Call<ApiResponse<ImportResultResponse>> importFlashCards(
            @Path("setId") String setId, @Body FlashCardImportRequest request);

    @PUT("flashcards/{id}")
    Call<ApiResponse<FlashCardResponse>> updateFlashCard(
            @Path("id") String id, @Body FlashCardRequest request);



    @GET("flashcards/suggest")
    Call<ApiResponse<SuggestResponse>> suggest(@Query("term") String term);

    // ── UC-STUDY-01: Study Today ───────────────────────────────────────
    @GET("study/today")
    Call<ApiResponse<StudyTodayResponse>> getStudyToday();

    // ── UC-STUDY-02~05: Start session ─────────────────────────────────
    @POST("study/session/start")
    Call<ApiResponse<StartSessionResponse>> startSession(@Body StartSessionRequest request);

    // ── UC-STUDY-06: End session + update streak ──────────────────────
    @PUT("study/session/{sessionId}/end")
    Call<ApiResponse<SessionResultResponse>> endSession(
            @Path("sessionId") String sessionId,
            @Body List<String> wrongTerms);

    // ── Submit Answer (cập nhật StudyRecord + SM-2) ───────────────────
    @POST("study/answer")
    Call<ApiResponse<Void>> submitAnswer(@Body AnswerRequest request);

    // ── UC-STAT-01,02,03: Statistics Summary ──────────────────────────
    @GET("study/stats/summary")
    Call<ApiResponse<StatSummaryResponse>> getStatSummary();

    // ── UC-STAT-04,05: Study Activity chart ───────────────────────────
    @GET("study/stats/activity")
    Call<ApiResponse<List<DayActivityResponse>>> getStudyActivity(
            @Query("period") String period  // "weekly" | "monthly"
    );

    // ── UC-STAT-06: Set Progress ──────────────────────────────────────
    @GET("study/stats/sets")
    Call<ApiResponse<List<SetProgressResponse>>> getSetProgress();
}