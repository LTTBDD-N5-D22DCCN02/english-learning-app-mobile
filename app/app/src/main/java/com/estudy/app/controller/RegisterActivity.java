package com.estudy.app.controller;

import android.app.DatePickerDialog;
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
import com.estudy.app.model.request.RegisterRequest;
import com.estudy.app.model.response.ApiResponse;
import com.estudy.app.model.response.UserResponse;
import com.estudy.app.utils.TokenManager;
import java.util.Calendar;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFullName, etUsername, etEmail, etPhone, etPassword, etConfirmPassword;
    private TextView tvDob, tvSignIn;
    private TextView tvErrorFullName, tvErrorUsername, tvErrorEmail,
            tvErrorPhone, tvErrorDob, tvErrorPassword, tvErrorConfirmPassword, tvErrorGeneral;
    private ImageButton btnTogglePassword, btnToggleConfirmPassword;
    private Button btnRegister;
    private ApiService apiService;

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    private String selectedDob = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        TokenManager tokenManager = new TokenManager(this);
        apiService = ApiClient.getInstance(tokenManager).create(ApiService.class);

        // Views
        etFullName = findViewById(R.id.etFullName);
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        tvDob = findViewById(R.id.tvDob);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        btnToggleConfirmPassword = findViewById(R.id.btnToggleConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvSignIn = findViewById(R.id.tvSignIn);

        // Error views
        tvErrorFullName = findViewById(R.id.tvErrorFullName);
        tvErrorUsername = findViewById(R.id.tvErrorUsername);
        tvErrorEmail = findViewById(R.id.tvErrorEmail);
        tvErrorPhone = findViewById(R.id.tvErrorPhone);
        tvErrorDob = findViewById(R.id.tvErrorDob);
        tvErrorPassword = findViewById(R.id.tvErrorPassword);
        tvErrorConfirmPassword = findViewById(R.id.tvErrorConfirmPassword);
        tvErrorGeneral = findViewById(R.id.tvErrorGeneral);

        // Date picker
        tvDob.setOnClickListener(v -> showDatePicker());

        // Toggle password
        btnTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            etPassword.setTransformationMethod(isPasswordVisible
                    ? HideReturnsTransformationMethod.getInstance()
                    : PasswordTransformationMethod.getInstance());
            etPassword.setSelection(etPassword.getText().length());
        });

        // Toggle confirm password
        btnToggleConfirmPassword.setOnClickListener(v -> {
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
            etConfirmPassword.setTransformationMethod(isConfirmPasswordVisible
                    ? HideReturnsTransformationMethod.getInstance()
                    : PasswordTransformationMethod.getInstance());
            etConfirmPassword.setSelection(etConfirmPassword.getText().length());
        });

        btnRegister.setOnClickListener(v -> handleRegister());
        tvSignIn.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    selectedDob = String.format("%04d-%02d-%02d",
                            selectedYear, selectedMonth + 1, selectedDay);
                    tvDob.setText(selectedDob);
                    tvDob.setTextColor(getResources().getColor(R.color.text_primary, null));
                    hideError(tvErrorDob);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        dialog.getDatePicker().setMaxDate(calendar.getTimeInMillis());
        dialog.show();
    }

    private void handleRegister() {
        // Reset tất cả lỗi
        clearErrors();

        String fullName = etFullName.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validate từng trường
        boolean hasError = false;

        if (fullName.isEmpty()) {
            showError(tvErrorFullName, "Full name must not be blank");
            hasError = true;
        }

        if (username.isEmpty()) {
            showError(tvErrorUsername, "Username must not be blank");
            hasError = true;
        } else if (username.length() < 3) {
            showError(tvErrorUsername, "Username must be at least 3 characters");
            hasError = true;
        }

        if (email.isEmpty()) {
            showError(tvErrorEmail, "Email must not be blank");
            hasError = true;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(tvErrorEmail, "Invalid email format");
            hasError = true;
        }

        if (phone.isEmpty()) {
            showError(tvErrorPhone, "Phone number must not be blank");
            hasError = true;
        } else if (!phone.matches("^[0-9]{10,11}$")) {
            showError(tvErrorPhone, "Phone number must be 10 to 11 digits");
            hasError = true;
        }

        if (selectedDob.isEmpty()) {
            showError(tvErrorDob, "Date of birth must not be blank");
            hasError = true;
        }

        if (password.isEmpty()) {
            showError(tvErrorPassword, "Password must not be blank");
            hasError = true;
        } else if (password.length() < 8) {
            showError(tvErrorPassword, "Password must be at least 8 characters");
            hasError = true;
        }

        if (confirmPassword.isEmpty()) {
            showError(tvErrorConfirmPassword, "Please confirm your password");
            hasError = true;
        } else if (!password.equals(confirmPassword)) {
            showError(tvErrorConfirmPassword, "Passwords do not match");
            hasError = true;
        }

        if (hasError) return;

        btnRegister.setEnabled(false);

        RegisterRequest request = new RegisterRequest(
                username, password, email, fullName, phone, selectedDob);

        apiService.register(request).enqueue(new Callback<ApiResponse<UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserResponse>> call,
                                   Response<ApiResponse<UserResponse>> response) {
                btnRegister.setEnabled(true);

                if (response.isSuccessful() && response.body() != null
                        && response.body().getResult() != null) {
                    // Thông báo thành công
                    showSuccess("Register successful! Please sign in.");
                    // Chờ 1.5 giây rồi chuyển sang Login
                    new android.os.Handler().postDelayed(() -> {
                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        finish();
                    }, 1500);
                } else {
                    // Lấy message lỗi từ server nếu có
                    String errorMsg = "Register failed. Please try again.";
                    if (response.body() != null && response.body().getMessage() != null) {
                        errorMsg = response.body().getMessage();
                    }
                    showError(tvErrorGeneral, errorMsg);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {
                btnRegister.setEnabled(true);
                showError(tvErrorGeneral, "Connection error. Please check your network.");
            }
        });
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private void showError(TextView tvError, String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void hideError(TextView tvError) {
        tvError.setText("");
        tvError.setVisibility(View.GONE);
    }

    private void showSuccess(String message) {
        tvErrorGeneral.setText(message);
        tvErrorGeneral.setVisibility(View.VISIBLE);
        tvErrorGeneral.setBackgroundResource(R.drawable.bg_success);
        tvErrorGeneral.setTextColor(getResources().getColor(android.R.color.white, null));
    }

    private void clearErrors() {
        hideError(tvErrorFullName);
        hideError(tvErrorUsername);
        hideError(tvErrorEmail);
        hideError(tvErrorPhone);
        hideError(tvErrorDob);
        hideError(tvErrorPassword);
        hideError(tvErrorConfirmPassword);
        // Reset về màu lỗi mặc định nếu đang hiển thị success
        tvErrorGeneral.setBackgroundResource(R.drawable.bg_error);
        tvErrorGeneral.setTextColor(
                getResources().getColor(android.R.color.holo_red_dark, null));
        hideError(tvErrorGeneral);
    }
}