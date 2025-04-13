package com.cntt2.flashcard.data.remote;

import com.cntt2.flashcard.data.remote.dto.CardDto;
import com.cntt2.flashcard.data.remote.dto.DeskDto;
import com.cntt2.flashcard.data.remote.dto.GetFolderDto;
import com.cntt2.flashcard.data.remote.dto.ImageDto;
import com.cntt2.flashcard.data.remote.dto.LoginRequest;
import com.cntt2.flashcard.data.remote.dto.LoginResponse;
import com.cntt2.flashcard.data.remote.dto.LogoutResponse;
import com.cntt2.flashcard.data.remote.dto.PostFolderDto;
import com.cntt2.flashcard.data.remote.dto.RegisterRequest;
import com.cntt2.flashcard.data.remote.dto.ReviewDto;
import com.cntt2.flashcard.data.remote.dto.SessionDto;

import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    // Auth endpoints
    @POST("api/auth/register")
    Call<Void> register(@Body RegisterRequest request);

    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/auth/logout")
    Call<LogoutResponse> logout();

    @GET("api/auth/me")
    Call<UserInfo> getCurrentUser();

    // Folder endpoints
    @POST("api/folder")
    Call<GetFolderDto> createFolder(@Body PostFolderDto folder);

    @GET("api/folder")
    Call<List<GetFolderDto>> getUserFolders();

    @PUT("api/folder/{id}")
    Call<Void> updateFolder(@Path("id") String id, @Body PostFolderDto folder);

    @DELETE("api/folder/{id}")
    Call<Void> deleteFolder(@Path("id") String id);

    // Desk endpoints
    @POST("api/desk")
    Call<DeskDto> createDesk(@Body DeskDto desk);

    @GET("api/desk")
    Call<List<DeskDto>> getUserDesks();

    @GET("api/desk/public")
    Call<List<DeskDto>> getPublicDesks();

    @PUT("api/desk/{id}")
    Call<Void> updateDesk(@Path("id") String id, @Body DeskDto desk);

    @POST("api/desk/{id}/clone")
    Call<DeskDto> cloneDesk(@Path("id") String id);

    @DELETE("api/desk/{id}")
    Call<Void> deleteDesk(@Path("id") String id);

    // Card endpoints
    @POST("api/card")
    Call<CardDto> createCard(@Body CardDto card);

    @GET("api/card")
    Call<List<CardDto>> getCardsByDeskId(@Query("deskId") String deskId);

    @PUT("api/card/{id}")
    Call<Void> updateCard(@Path("id") String id, @Body CardDto card);

    @DELETE("api/card/{id}")
    Call<Void> deleteCard(@Path("id") String id);

    // Review endpoints
    @POST("api/review")
    Call<ReviewDto> createReview(@Body ReviewDto reviewDto);

    @GET("api/review/card/{cardId}")
    Call<ReviewDto> getReviewByCardId(@Path("cardId") String cardId);

    @PUT("api/review/{id}")
    Call<Void> updateReview(@Path("id") String id, @Body ReviewDto reviewDto);

    @DELETE("api/review/{id}")
    Call<Void> deleteReview(@Path("id") String id);

    @GET("api/review/due-today")
    Call<List<ReviewDto>> getReviewsDueToday(@Query("deskId") String deskId, @Query("today") String today);

    // Session endpoints
    @POST("api/session")
    Call<SessionDto> createSession(@Body SessionDto session);

    @GET("api/session/desk/{deskId}")
    Call<List<SessionDto>> getSessionsByDeskId(@Path("deskId") String deskId);

    @PUT("api/session/{id}")
    Call<Void> updateSession(@Path("id") String id, @Body SessionDto session);

    @DELETE("api/session/{id}")
    Call<Void> deleteSession(@Path("id") String id);

    // Image endpoints
    @Multipart
    @POST("api/image/upload")
    Call<ImageDto> uploadImage(@Part MultipartBody.Part file);

    @DELETE("api/image/{id}")
    Call<Void> deleteImage(@Path("id") String id);
}