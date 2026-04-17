package com.estudy.app.controller;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.estudy.app.R;
import com.estudy.app.api.ApiClient;
import com.estudy.app.api.ApiService;
import com.estudy.app.model.request.UpdateProfileRequest;
import com.estudy.app.model.response.ApiResponse;
import com.estudy.app.model.response.UserResponse;
import com.estudy.app.utils.BottomNavHelper;
import com.estudy.app.utils.TokenManager;

import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private TextView  tvAvatarInitial, tvUsername;
    private EditText  etFullName, etDob, etPhone, etEmail;
    private Button    btnAction;
    private ProgressBar progressBar;
    private ScrollView scrollContent;

    private ApiService   apiService;
    private TokenManager tokenManager;

    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        BottomNavHelper.setup(this, -1);

        tokenManager = new TokenManager(this);
        apiService   = ApiClient.getInstance(tokenManager).create(ApiService.class);

        bindViews();
        setupListeners();
        loadProfile();
    }

    // ─────────────────────────────────────────────────
    // Bind views
    // ─────────────────────────────────────────────────
    private void bindViews() {
        tvAvatarInitial = findViewById(R.id.tvAvatarInitial);
        tvUsername      = findViewById(R.id.tvUsername);
        etFullName      = findViewById(R.id.etFullName);
        etDob           = findViewById(R.id.etDob);
        etPhone         = findViewById(R.id.etPhone);
        etEmail         = findViewById(R.id.etEmail);
        btnAction       = findViewById(R.id.btnAction);
        progressBar     = findViewById(R.id.progressBar);
        scrollContent   = findViewById(R.id.scrollContent);
    }

    private void setupListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // DatePicker cho trường ngày sinh (chỉ hoạt động khi edit mode)
        etDob.setOnClickListener(v -> {
            if (!isEditMode) return;
            showDatePicker();
        });

        btnAction.setOnClickListener(v -> {
            if (isEditMode) {
                saveProfile();
            } else {
                enterEditMode();
            }
        });
    }

    // ─────────────────────────────────────────────────
    // UC-01: Load thông tin cá nhân
    // ─────────────────────────────────────────────────
    private void loadProfile() {
        showLoading(true);
        apiService.getMyProfile().enqueue(new Callback<ApiResponse<UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserResponse>> call,
                                   Response<ApiResponse<UserResponse>> response) {
                showLoading(false);
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().getResult() != null) {
                    populateViews(response.body().getResult());
                } else {
                    showRetryError();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {
                showLoading(false);
                showRetryError();
            }
        });
    }

    private void populateViews(UserResponse user) {
        String username = user.getUsername() != null ? user.getUsername() : "";
        tvUsername.setText(username);
        tvAvatarInitial.setText(username.isEmpty() ? "?" : String.valueOf(username.charAt(0)).toUpperCase());

        etFullName.setText(user.getFullName() != null ? user.getFullName() : "");
        etDob.setText(user.getDob() != null && !user.getDob().isEmpty() ? user.getDob() : "");
        etPhone.setText(user.getPhone() != null ? user.getPhone() : "");
        etEmail.setText(user.getEmail() != null ? user.getEmail() : "");

        // Hiển thị "Chưa cập nhật" làm hint cho field chưa có dữ liệu
        etFullName.setHint(user.getFullName() == null  ? "Chưa cập nhật" : "");
        etDob.setHint(user.getDob() == null            ? "Chưa cập nhật" : "yyyy-MM-dd");
        etPhone.setHint(user.getPhone() == null        ? "Chưa cập nhật" : "");
        etEmail.setHint(user.getEmail() == null        ? "Chưa cập nhật" : "");
    }

    // ─────────────────────────────────────────────────
    // UC-02: Chế độ chỉnh sửa
    // ─────────────────────────────────────────────────
    private void enterEditMode() {
        isEditMode = true;
        setFieldsEditable(true);
        btnAction.setText("SAVE CHANGE");
    }

    private void exitEditMode() {
        isEditMode = false;
        setFieldsEditable(false);
        btnAction.setText("Update profile");
    }

    private void setFieldsEditable(boolean editable) {
        etFullName.setFocusable(editable);
        etFullName.setFocusableInTouchMode(editable);
        etPhone.setFocusable(editable);
        etPhone.setFocusableInTouchMode(editable);
        etEmail.setFocusable(editable);
        etEmail.setFocusableInTouchMode(editable);
        // etDob không setFocusable vì dùng DatePickerDialog qua onClick
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();

        // Parse ngày hiện tại nếu có
        String current = etDob.getText().toString().trim();
        if (current.matches("\\d{4}-\\d{2}-\\d{2}")) {
            String[] parts = current.split("-");
            cal.set(Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]) - 1,
                    Integer.parseInt(parts[2]));
        }

        new DatePickerDialog(this,
                (view, year, month, day) -> {
                    String dob = String.format("%04d-%02d-%02d", year, month + 1, day);
                    etDob.setText(dob);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    // ─────────────────────────────────────────────────
    // UC-02: Gọi API lưu thông tin
    // ─────────────────────────────────────────────────
    private void saveProfile() {
        String fullName = etFullName.getText().toString().trim();
        String email    = etEmail.getText().toString().trim();
        String phone    = etPhone.getText().toString().trim();
        String dob      = etDob.getText().toString().trim();

        // Validation
        if (fullName.isEmpty()) {
            etFullName.setError("Họ tên không được để trống");
            etFullName.requestFocus();
            return;
        }
        if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không đúng định dạng");
            etEmail.requestFocus();
            return;
        }
        if (!phone.isEmpty() && !phone.matches("^[0-9]{10,11}$")) {
            etPhone.setError("Số điện thoại phải có 10–11 chữ số");
            etPhone.requestFocus();
            return;
        }

        UpdateProfileRequest request = new UpdateProfileRequest(
                fullName,
                email.isEmpty()  ? null : email,
                phone.isEmpty()  ? null : phone,
                dob.isEmpty()    ? null : dob
        );

        showLoading(true);
        apiService.updateProfile(request).enqueue(new Callback<ApiResponse<UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserResponse>> call,
                                   Response<ApiResponse<UserResponse>> response) {
                showLoading(false);
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().getResult() != null) {
                    Toast.makeText(ProfileActivity.this,
                            "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    populateViews(response.body().getResult());
                    exitEditMode();
                } else {
                    String msg = "Cập nhật thất bại";
                    if (response.body() != null && response.body().getMessage() != null)
                        msg = response.body().getMessage();
                    Toast.makeText(ProfileActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {
                showLoading(false);
                Toast.makeText(ProfileActivity.this,
                        "Lỗi kết nối. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────
    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        scrollContent.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private void showRetryError() {
        Toast.makeText(this,
                "Không tải được dữ liệu. Nhấn Back và thử lại.",
                Toast.LENGTH_LONG).show();
    }
}
