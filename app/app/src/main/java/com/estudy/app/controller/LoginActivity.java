package com.estudy.app.controller;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.estudy.app.R;
import com.estudy.app.api.ApiClient;
import com.estudy.app.api.ApiService;
import com.estudy.app.model.request.LoginRequest;
import com.estudy.app.model.response.ApiResponse;
import com.estudy.app.model.response.AuthResponse;
import com.estudy.app.utils.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private TextView tvErrorUsername, tvErrorPassword, tvErrorGeneral, tvSignUp;
    private ImageButton btnTogglePassword;
    private Button btnLogin;
    private TokenManager tokenManager;
    private ApiService apiService;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tokenManager = new TokenManager(this);
        apiService = ApiClient.getInstance(tokenManager).create(ApiService.class);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        tvErrorUsername = findViewById(R.id.tvErrorUsername);
        tvErrorPassword = findViewById(R.id.tvErrorPassword);
        tvErrorGeneral = findViewById(R.id.tvErrorGeneral);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUp = findViewById(R.id.tvSignUp);

        // Toggle password visibility
        btnTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            etPassword.setTransformationMethod(isPasswordVisible
                    ? HideReturnsTransformationMethod.getInstance()
                    : PasswordTransformationMethod.getInstance());
            etPassword.setSelection(etPassword.getText().length());
        });

        btnLogin.setOnClickListener(v -> handleLogin());
        tvSignUp.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void handleLogin() {
        // Reset lỗi cũ
        clearErrors();

        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate từng trường
        boolean hasError = false;

        if (username.isEmpty()) {
            showError(tvErrorUsername, "Username must not be blank");
            hasError = true;
        }

        if (password.isEmpty()) {
            showError(tvErrorPassword, "Password must not be blank");
            hasError = true;
        }

        if (hasError) return;

        btnLogin.setEnabled(false);
        hideError(tvErrorGeneral);

        apiService.login(new LoginRequest(username, password))
                .enqueue(new Callback<ApiResponse<AuthResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<AuthResponse>> call,
                                           Response<ApiResponse<AuthResponse>> response) {
                        btnLogin.setEnabled(true);

                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResult() != null) {
                            // Đăng nhập thành công
                            tokenManager.saveToken(response.body().getResult().getToken());
                            ApiClient.reset();
                            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                            finish();
                        } else {
                            // Sai username hoặc password
                            showError(tvErrorGeneral, "Invalid username or password. Please try again.");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                        btnLogin.setEnabled(true);
                        showError(tvErrorGeneral, "Connection error. Please check your network.");
                    }
                });
    }

    private void showError(TextView tvError, String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void hideError(TextView tvError) {
        tvError.setText("");
        tvError.setVisibility(View.GONE);
    }

    private void clearErrors() {
        hideError(tvErrorUsername);
        hideError(tvErrorPassword);
        hideError(tvErrorGeneral);
    }
}