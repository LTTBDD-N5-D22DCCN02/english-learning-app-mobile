package com.estudy.app.api;

import com.estudy.app.model.request.CommentRequest;
import com.estudy.app.model.request.FlashCardSetRequest;
import com.estudy.app.model.request.LoginRequest;
import com.estudy.app.model.request.LogoutRequest;
import com.estudy.app.model.request.RegisterRequest;
import com.estudy.app.model.response.ApiResponse;
import com.estudy.app.model.response.AuthResponse;
import com.estudy.app.model.response.CommentResponse;
import com.estudy.app.model.response.FlashCardSetDetailResponse;
import com.estudy.app.model.response.FlashCardSetResponse;
import com.estudy.app.model.response.UserResponse;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    // Auth
    @POST("auth/register")
    Call<ApiResponse<UserResponse>> register(@Body RegisterRequest request);

    @POST("auth/login")
    Call<ApiResponse<AuthResponse>> login(@Body LoginRequest request);

    @POST("auth/logout")
    Call<ApiResponse<Void>> logout(@Body LogoutRequest request);

    // FlashCard Set
    @GET("flashcard-sets/my")
    Call<ApiResponse<List<FlashCardSetResponse>>> getMyFlashCardSets();

    @GET("flashcard-sets/{id}")
    Call<ApiResponse<FlashCardSetDetailResponse>> getFlashCardSetDetail(@Path("id") String id);

    @POST("flashcard-sets")
    Call<ApiResponse<FlashCardSetResponse>> createFlashCardSet(@Body FlashCardSetRequest request);

    @PUT("flashcard-sets/{id}")
    Call<ApiResponse<FlashCardSetResponse>> updateFlashCardSet(@Path("id") String id, @Body FlashCardSetRequest request);

    @DELETE("flashcard-sets/{id}")
    Call<ApiResponse<Void>> deleteFlashCardSet(@Path("id") String id);

    // Comment
    @POST("flashcard-sets/{flashCardSetId}/comments")
    Call<ApiResponse<CommentResponse>> addComment(@Path("flashCardSetId") String flashCardSetId, @Body CommentRequest request);

    @DELETE("flashcard-sets/comments/{commentId}")
    Call<ApiResponse<Void>> deleteComment(@Path("commentId") String commentId);
}