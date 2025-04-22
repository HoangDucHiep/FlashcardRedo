package com.cntt2.flashcard.data.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.auth.AuthManager;
import com.cntt2.flashcard.data.remote.dto.LogoutResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String BASE_URL = "http://10.0.2.2:5029/";
    private static Retrofit retrofit;
    private static AuthManager authManager;
    private static ApiService apiService;

    public static void init(Context context) {
        if (authManager == null) {
            authManager = new AuthManager(context);
        }
        if (apiService == null) {
            apiService = getApiService();
        }
    }

    public static ApiService getApiService() {
        if (retrofit == null) {
            OkHttpClient client = getUnsafeOkHttpClient();

            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit.create(ApiService.class);
    }

    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            };

            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            final HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Đảm bảo authManager đã được khởi tạo trước khi tạo Interceptor
            if (authManager == null) {
                throw new IllegalStateException("AuthManager is not initialized. Call ApiClient.init() first.");
            }

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier(hostnameVerifier)
                    .addInterceptor(new AuthInterceptor())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class AuthInterceptor implements Interceptor {
        @Override
        public okhttp3.Response intercept(Chain chain) throws java.io.IOException {
            String token = authManager.getToken();
            Request originalRequest = chain.request();
            if (token != null) {
                Request newRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer " + token)
                        .build();
                return chain.proceed(newRequest);
            }
            return chain.proceed(originalRequest);
        }
    }

    public static void saveAuthData(String token, String username, String userId, String email) {
        if (authManager == null) {
            throw new IllegalStateException("AuthManager is not initialized. Call ApiClient.init() first.");
        }
        authManager.saveAuthData(token, username, userId, email);
    }

    public static String getToken() {
        if (authManager == null) {
            throw new IllegalStateException("AuthManager is not initialized. Call ApiClient.init() first.");
        }
        return authManager.getToken();
    }

    public static String getUserId() {
        if (authManager == null) {
            throw new IllegalStateException("AuthManager is not initialized. Call ApiClient.init() first.");
        }
        return authManager.getUserId();
    }

    public static boolean isLoggedIn() {
        if (authManager == null) {
            return false; // Trả về false nếu authManager chưa được khởi tạo
        }
        return authManager.isLoggedIn();
    }

    public static void logout() {
        if (authManager == null) {
            Log.w("ApiClient", "AuthManager is null, cannot logout");
            return;
        }
        authManager.logout();
    }

    public static void logout(final LogoutCallback callback) {
        if (apiService == null) {
            Log.w("ApiClient", "apiService is null, skipping API logout");
            callback.onFailure("API service not initialized");
            return;
        }
        Call<LogoutResponse> call = apiService.logout();
        call.enqueue(new Callback<LogoutResponse>() {
            @Override
            public void onResponse(Call<LogoutResponse> call, Response<LogoutResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("ApiClient", "Logout successful: " + response.body().getMessage());
                    authManager.logout(); // Xóa token và thông tin user
                    callback.onSuccess();
                } else {
                    Log.e("ApiClient", "Logout failed: " + response.code() + " - " + response.message());
                    try {
                        Log.e("ApiClient", "Error body: " + response.errorBody().string());
                    } catch (Exception e) {
                        Log.e("ApiClient", "Error reading error body: " + e.getMessage());
                    }
                    callback.onFailure("Logout failed: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<LogoutResponse> call, Throwable t) {
                Log.e("ApiClient", "Logout error: " + t.getMessage());
                callback.onFailure("Logout error: " + t.getMessage());
            }
        });
    }

    public static void checkLoginStatus(final LoginStatusCallback callback) {
        if (!isLoggedIn()) {
            callback.onFailure("No token found");
            return;
        }

        if (apiService == null) {
            Log.w("ApiClient", "apiService is null, cannot check login status");
            callback.onFailure("API service not initialized");
            return;
        }

        Call<UserInfo> call = apiService.getCurrentUser();
        call.enqueue(new Callback<UserInfo>() {
            @Override
            public void onResponse(Call<UserInfo> call, Response<UserInfo> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Token hợp lệ, người dùng đã đăng nhập
                    callback.onSuccess(response.body());
                } else {
                    // Token không hợp lệ hoặc lỗi server
                    authManager.logout(); // Xóa token không hợp lệ
                    callback.onFailure("Invalid token or server error");
                }
            }

            @Override
            public void onFailure(Call<UserInfo> call, Throwable t) {
                // Lỗi kết nối
                authManager.logout(); // Xóa token để yêu cầu đăng nhập lại
                callback.onFailure("Connection error: " + t.getMessage());
            }
        });
    }

    public interface LoginStatusCallback {
        void onSuccess(UserInfo userInfo);
        void onFailure(String error);
    }

    public interface LogoutCallback {
        void onSuccess();
        void onFailure(String error);
    }
}