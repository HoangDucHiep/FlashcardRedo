package com.cntt2.flashcard.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.data.remote.ApiService;
import com.cntt2.flashcard.data.remote.dto.LogoutResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthManager {
    private static final String PREFS_NAME = "auth_prefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "email";

    private final SharedPreferences prefs;
    private final ApiService apiService;

    public AuthManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        apiService = App.getInstance().getApiService();
    }

    public void saveAuthData(String token, String username, String userId, String email) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_TOKEN, token);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public void logout() {
        // Kiểm tra apiService trước khi gọi logout
        if (apiService != null) {
            Call<LogoutResponse> call = apiService.logout();
            call.enqueue(new Callback<LogoutResponse>() {
                @Override
                public void onResponse(Call<LogoutResponse> call, Response<LogoutResponse> response) {
                    // Dù API thành công hay thất bại, xóa dữ liệu local
                    clearAuthData();
                }

                @Override
                public void onFailure(Call<LogoutResponse> call, Throwable t) {
                    // Nếu API thất bại, vẫn xóa dữ liệu local
                    clearAuthData();
                    Log.e("AuthManager", "Logout API failed: " + t.getMessage());
                }
            });
        } else {
            // Nếu apiService null, chỉ xóa dữ liệu local
            clearAuthData();
            Log.w("AuthManager", "apiService is null, skipping API logout");
        }
    }

    public void logout(LogoutCallback callback) {
        // Kiểm tra apiService trước khi gọi logout
        if (apiService == null) {
            clearAuthData();
            callback.onFailure("API service not initialized");
            Log.w("AuthManager", "apiService is null, skipping API logout");
            return;
        }

        Call<LogoutResponse> call = apiService.logout();
        call.enqueue(new Callback<LogoutResponse>() {
            @Override
            public void onResponse(Call<LogoutResponse> call, Response<LogoutResponse> response) {
                // Xóa dữ liệu local
                clearAuthData();
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().getMessage());
                } else {
                    callback.onFailure("Logout failed: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<LogoutResponse> call, Throwable t) {
                // Xóa dữ liệu local ngay cả khi API thất bại
                clearAuthData();
                callback.onFailure("Logout error: " + t.getMessage());
            }
        });
    }

    private void clearAuthData() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        Log.d("AuthManager", "Auth data cleared");
    }

    public interface LogoutCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }
}