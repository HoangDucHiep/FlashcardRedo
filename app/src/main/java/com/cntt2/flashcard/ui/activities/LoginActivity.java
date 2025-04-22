package com.cntt2.flashcard.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cntt2.flashcard.App;
import com.cntt2.flashcard.R;
import com.cntt2.flashcard.data.remote.ApiClient;
import com.cntt2.flashcard.data.remote.ApiService;
import com.cntt2.flashcard.data.remote.UserInfo;
import com.cntt2.flashcard.data.remote.dto.LoginRequest;
import com.cntt2.flashcard.data.remote.dto.LoginResponse;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends BaseActivity {
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView registerText;
    private TextView forgotPasswordText;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Khởi tạo UI
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        loginButton = findViewById(R.id.login_button);
        registerText = findViewById(R.id.register_text);
        forgotPasswordText = findViewById(R.id.forgot_password_text);
        apiService = App.getInstance().getApiService();

        // Kiểm tra trạng thái đăng nhập
        checkLoginStatus();

        // Xử lý sự kiện nhấn nút Login
        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            if (!email.isEmpty() && !password.isEmpty()) {
                login(email, password);
            } else {
                Toast.makeText(this, "Please fill email and password", Toast.LENGTH_SHORT).show();
            }
        });

        // Chuyển sang màn hình Đăng ký
        registerText.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        forgotPasswordText.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });
    }

    private void checkLoginStatus() {
        ApiClient.checkLoginStatus(new ApiClient.LoginStatusCallback() {
            @Override
            public void onSuccess(UserInfo userInfo) {
                // Token hợp lệ, chuyển sang MainActivity
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onFailure(String error) {
                // Token không hợp lệ hoặc lỗi kết nối, giữ nguyên màn hình đăng nhập
                Toast.makeText(LoginActivity.this, "Please log in: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void login(String email, String password) {
        LoginRequest loginRequest = new LoginRequest(email, password);
        Call<LoginResponse> call = apiService.login(loginRequest);

        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    String token = loginResponse.getToken();
                    String username = loginResponse.getUsername();
                    String email = loginResponse.getEmail();

                    // Lưu token tạm thời vào AuthManager trước khi gọi getCurrentUser
                    ApiClient.saveAuthData(token, username, null, email);

                    // Gọi API để lấy thông tin user (userId)
                    fetchUserInfo(token, username, email);
                } else {
                    String errorMessage = "Login failed";
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            JSONObject jsonObject = new JSONObject(errorBody);
                            errorMessage = jsonObject.getString("message");
                        } catch (Exception e) {
                            errorMessage = "Error parsing server response";
                        }
                    }
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Connection error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fetchUserInfo(String token, String username, String email) {
        Call<UserInfo> call = apiService.getCurrentUser();
        call.enqueue(new Callback<UserInfo>() {
            @Override
            public void onResponse(Call<UserInfo> call, Response<UserInfo> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserInfo userInfo = response.body();
                    String userId = userInfo.getId();

                    // Cập nhật lại thông lexical knowledge tin đăng nhập với userId
                    ApiClient.saveAuthData(token, username, userId, email);

                    // Chuyển đến MainActivity
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    String errorMessage = "Can't retrieve user information";
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            JSONObject jsonObject = new JSONObject(errorBody);
                            errorMessage = jsonObject.getString("message");
                        } catch (Exception e) {
                            errorMessage = "Error parsing server response";
                        }
                    }
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserInfo> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Connection error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}