package com.estudy.app.api;

import com.estudy.app.model.request.ClassFlashCardSetRequest;
import com.estudy.app.model.request.ClassRequest;
import com.estudy.app.model.request.AnswerRequest;
import com.estudy.app.model.request.CommentRequest;
import com.estudy.app.model.request.CopyClassRequest;
import com.estudy.app.model.request.FlashCardImportRequest;
import com.estudy.app.model.request.FlashCardRequest;
import com.estudy.app.model.request.FlashCardSetRequest;
import com.estudy.app.model.request.JoinClassRequest;
import com.estudy.app.model.request.LoginRequest;
import com.estudy.app.model.request.LogoutRequest;
import com.estudy.app.model.request.RegisterRequest;
import com.estudy.app.model.request.UpdateMemberRoleRequest;
import com.estudy.app.model.request.StartSessionRequest;
import com.estudy.app.model.response.ApiResponse;
import com.estudy.app.model.response.AuthResponse;
import com.estudy.app.model.response.ClassMemberResponse;
import com.estudy.app.model.response.ClassResponse;
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

    // ── UC-01: Tạo lớp học ───────────────────────────────────────────
    @POST("classes")
    Call<ApiResponse<ClassResponse>> createClass(@Body ClassRequest request);

    // ── UC-02: Sửa thông tin lớp học ────────────────────────────────
    @PUT("classes/{classId}")
    Call<ApiResponse<ClassResponse>> updateClass(
            @Path("classId") String classId, @Body ClassRequest request);

    // ── UC-03: Xóa lớp học ───────────────────────────────────────────
    @DELETE("classes/{classId}")
    Call<ApiResponse<Void>> deleteClass(@Path("classId") String classId);

    // ── UC-04: Danh sách lớp đã tham gia ────────────────────────────
    @GET("classes/my")
    Call<ApiResponse<List<ClassResponse>>> getMyClasses();

    // ── UC-05: Tham gia bằng mã lớp ─────────────────────────────────
    @POST("classes/join")
    Call<ApiResponse<Void>> joinClass(@Body JoinClassRequest request);

    // ── UC-06: Rời lớp học ───────────────────────────────────────────
    @DELETE("classes/{classId}/leave")
    Call<ApiResponse<Void>> leaveClass(@Path("classId") String classId);

    // ── UC-07/08: Chi tiết lớp học ───────────────────────────────────
    @GET("classes/{classId}")
    Call<ApiResponse<ClassResponse>> getClassDetail(@Path("classId") String classId);

    // ── UC-09: Copy lớp học ──────────────────────────────────────────
    @POST("classes/{classId}/copy")
    Call<ApiResponse<ClassResponse>> copyClass(
            @Path("classId") String classId, @Body CopyClassRequest request);

    // ── UC-10: Tìm kiếm lớp học ─────────────────────────────────────
    @GET("classes/search")
    Call<ApiResponse<List<ClassResponse>>> searchMyClasses(@Query("keyword") String keyword);

    // ── UC-11: Tham gia lớp công khai ───────────────────────────────
    @POST("classes/{classId}/join-public")
    Call<ApiResponse<Void>> joinPublicClass(@Path("classId") String classId);

    // ── UC-12: Danh sách lớp công khai ──────────────────────────────
    @GET("classes/public")
    Call<ApiResponse<List<ClassResponse>>> getPublicClasses(@Query("keyword") String keyword);

    // ── UC-13/23: Danh sách & tìm kiếm thành viên ───────────────────
    @GET("classes/{classId}/members")
    Call<ApiResponse<List<ClassMemberResponse>>> getMembers(@Path("classId") String classId);

    @GET("classes/{classId}/members/search")
    Call<ApiResponse<List<ClassMemberResponse>>> searchMembers(
            @Path("classId") String classId, @Query("keyword") String keyword);

    // ── UC-14: Danh sách yêu cầu chờ duyệt ─────────────────────────
    @GET("classes/{classId}/members/pending")
    Call<ApiResponse<List<ClassMemberResponse>>> getPendingRequests(
            @Path("classId") String classId);

    // ── UC-14: Duyệt yêu cầu ────────────────────────────────────────
    @PATCH("classes/{classId}/members/{memberId}/approve")
    Call<ApiResponse<Void>> approveRequest(
            @Path("classId") String classId, @Path("memberId") String memberId);

    // ── UC-14: Từ chối yêu cầu ──────────────────────────────────────
    @PATCH("classes/{classId}/members/{memberId}/reject")
    Call<ApiResponse<Void>> rejectRequest(
            @Path("classId") String classId, @Path("memberId") String memberId);

    // ── UC-15: Xóa thành viên ────────────────────────────────────────
    @DELETE("classes/{classId}/members/{userId}")
    Call<ApiResponse<Void>> removeMember(
            @Path("classId") String classId, @Path("userId") String userId);

    // ── UC-16: Lấy mã lớp ───────────────────────────────────────────
    @GET("classes/{classId}/code")
    Call<ApiResponse<String>> getClassCode(@Path("classId") String classId);

    // ── UC-17/22: Flashcard Sets trong lớp ──────────────────────────
    @GET("classes/{classId}/flashcard-sets")
    Call<ApiResponse<List<FlashCardSetResponse>>> getClassFlashCardSets(
            @Path("classId") String classId);

    @GET("classes/{classId}/flashcard-sets/search")
    Call<ApiResponse<List<FlashCardSetResponse>>> searchClassFlashCardSets(
            @Path("classId") String classId, @Query("keyword") String keyword);

    // ── UC-18: Thêm Flashcard Set vào lớp ───────────────────────────
    @POST("classes/{classId}/flashcard-sets")
    Call<ApiResponse<FlashCardSetResponse>> addClassFlashCardSet(
            @Path("classId") String classId, @Body ClassFlashCardSetRequest request);


    // ── UC-19: Sửa Flashcard Set trong lớp ──────────────────────────
    @PUT("classes/{classId}/flashcard-sets/{setId}")
    Call<ApiResponse<FlashCardSetResponse>> updateClassFlashCardSet(
            @Path("classId") String classId,
            @Path("setId") String setId,
            @Body ClassFlashCardSetRequest request);

    // ── UC-20: Xóa Flashcard Set khỏi lớp ───────────────────────────
    @DELETE("classes/{classId}/flashcard-sets/{setId}")
    Call<ApiResponse<Void>> deleteClassFlashCardSet(
            @Path("classId") String classId, @Path("setId") String setId);

    // ── Lấy chi tiết Flashcard Set từ lớp (nếu backend hỗ trợ) ──────
    @GET("classes/{classId}/flashcard-sets/{setId}")
    Call<ApiResponse<FlashCardSetDetailResponse>> getClassFlashCardSetDetail(
            @Path("classId") String classId, @Path("setId") String setId);

    // ── UC-24: Cập nhật quyền thành viên ────────────────────────────
    @PATCH("classes/{classId}/members/{userId}/role")
    Call<ApiResponse<Void>> updateMemberRole(
            @Path("classId") String classId,
            @Path("userId") String userId,
            @Body UpdateMemberRoleRequest request);
    @GET("auth/me")
    Call<ApiResponse<UserResponse>> getCurrentUser();


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