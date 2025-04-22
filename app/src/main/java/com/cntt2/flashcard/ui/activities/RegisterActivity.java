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
import com.cntt2.flashcard.data.remote.dto.RegisterRequest;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends BaseActivity {
    private EditText usernameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private Button registerButton;
    private TextView loginText;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize UI
        usernameEditText = findViewById(R.id.username_edit_text);
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        confirmPasswordEditText = findViewById(R.id.confirm_password_edit_text);
        registerButton = findViewById(R.id.register_button);
        loginText = findViewById(R.id.login_text);
        apiService = App.getInstance().getApiService();

        // Handle Register button click
        registerButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            register(username, email, password);
        });

        // Navigate to Login screen
        loginText.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });
    }

    private void register(String username, String email, String password) {
        RegisterRequest registerRequest = new RegisterRequest(username, email, password);
        Call<Void> call = apiService.register(registerRequest);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, "Registration successful! Logging in...", Toast.LENGTH_SHORT).show();
                    // Automatically log in after successful registration
                    loginAfterRegister(email, password);
                } else {
                    String errorMessage = "Registration failed";
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            JSONObject jsonObject = new JSONObject(errorBody);
                            errorMessage = jsonObject.getString("message");
                        } catch (Exception e) {
                            errorMessage = "Error parsing server response";
                        }
                    }
                    Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "Connection error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loginAfterRegister(String email, String password) {
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

                    // Temporarily save token in AuthManager before calling getCurrentUser
                    ApiClient.saveAuthData(token, username, null, email);

                    // Call API to fetch user information (userId)
                    fetchUserInfo(token, username, email);
                } else {
                    String errorMessage = "Login after registration failed";
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            JSONObject jsonObject = new JSONObject(errorBody);
                            errorMessage = jsonObject.getString("message");
                        } catch (Exception e) {
                            errorMessage = "Error parsing server response";
                        }
                    }
                    Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "Connection error: " + t.getMessage(), Toast.LENGTH_LONG).show();
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

                    // Update login information with userId
                    ApiClient.saveAuthData(token, username, userId, email);

                    // Navigate to SplashActivity for synchronization
                    startActivity(new Intent(RegisterActivity.this, SplashActivity.class));
                    finish();
                } else {
                    String errorMessage = "Unable to fetch user information";
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            JSONObject jsonObject = new JSONObject(errorBody);
                            errorMessage = jsonObject.getString("message");
                        } catch (Exception e) {
                            errorMessage = "Error parsing server response";
                        }
                    }
                    Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show();

                }
            }

            @Override
            public void onFailure(Call<UserInfo> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "Connection error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}