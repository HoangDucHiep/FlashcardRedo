package com.cntt2.flashcard.data.remote;

import android.content.Context;
import android.content.SharedPreferences;

import com.cntt2.flashcard.App;

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
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String BASE_URL = "https://10.0.2.2:7198/";
    private static Retrofit retrofit;

    public static ApiService getApiService() {
        if (retrofit == null) {
            OkHttpClient client = getUnsafeOkHttpClient();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }

    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Tạo TrustManager bỏ qua kiểm tra chứng chỉ
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

            // Cài đặt SSLContext
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // Tạo HostnameVerifier bỏ qua kiểm tra hostname
            final HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

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
            SharedPreferences prefs = App.getInstance().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
            String token = prefs.getString("auth_token", null);
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

    public static void saveToken(String token) {
        SharedPreferences prefs = App.getInstance().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        prefs.edit().putString("auth_token", token).apply();
    }

    public static String getToken() {
        SharedPreferences prefs = App.getInstance().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        return prefs.getString("auth_token", null);
    }
}