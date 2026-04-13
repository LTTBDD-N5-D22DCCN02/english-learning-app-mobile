package com.estudy.app.controller;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.estudy.app.R;
import com.estudy.app.api.ApiClient;
import com.estudy.app.api.ApiService;
import com.estudy.app.model.response.ApiResponse;
import com.estudy.app.model.response.ClassResponse;
import com.estudy.app.utils.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * UC-S1: Xem thông tin lớp học (Setting)
 * UC-S2: Rời lớp học từ màn hình Setting (Member)
 * UC-S3: Xóa lớp học từ màn hình Setting (Leader)
 */
public class ClassSettingActivity extends AppCompatActivity {

    private String classId, className;
    private ApiService apiService;
    private TokenManager tokenManager;

    private TextView tvClassName, tvDescription, tvPrivacy, tvMemberCount, tvClassCode;
    private Button btnLeaveOrDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_setting);

        classId   = getIntent().getStringExtra("classId");
        className = getIntent().getStringExtra("className");

        tokenManager = new TokenManager(this);
        apiService   = ApiClient.getInstance(tokenManager).create(ApiService.class);

        View toolbar = findViewById(R.id.toolbar);
        ((ImageButton) toolbar.findViewById(R.id.btnBack)).setOnClickListener(v -> finish());
        ((TextView) toolbar.findViewById(R.id.tvToolbarTitle)).setText("Setting");

        tvClassName   = findViewById(R.id.tvClassName);
        tvDescription = findViewById(R.id.tvDescription);
        tvPrivacy     = findViewById(R.id.tvPrivacy);
        tvMemberCount = findViewById(R.id.tvMemberCount);
        tvClassCode   = findViewById(R.id.tvClassCode);
        btnLeaveOrDelete = findViewById(R.id.btnLeaveOrDelete);

        loadClassInfo();
    }

    private void loadClassInfo() {
        apiService.getClassDetail(classId)
                .enqueue(new Callback<ApiResponse<ClassResponse>>() {
                    public void onResponse(Call<ApiResponse<ClassResponse>> call,
                                           Response<ApiResponse<ClassResponse>> r) {
                        if (r.isSuccessful() && r.body() != null && r.body().getResult() != null) {
                            ClassResponse cls = r.body().getResult();
                            runOnUiThread(() -> bindClassInfo(cls));
                        }
                    }
                    public void onFailure(Call<ApiResponse<ClassResponse>> call, Throwable t) {
                        Toast.makeText(ClassSettingActivity.this, "Load error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void bindClassInfo(ClassResponse cls) {
        tvClassName.setText(cls.getName());
        tvDescription.setText(cls.getDescription() != null ? cls.getDescription() : "-");
        tvPrivacy.setText(cls.getPrivacy() != null ? cls.getPrivacy() : "-");
        tvMemberCount.setText(String.valueOf(cls.getMemberCount()));

        boolean isLeader = "LEADER".equals(cls.getMyRole());

        // Hiển thị class code nếu là Leader
        if (isLeader) {
            loadClassCode();
        } else {
            tvClassCode.setText("(Only visible to Leader)");
        }

        // Nút: Leader → Delete Class, Member → Leave Class
        if (isLeader) {
            btnLeaveOrDelete.setText("Delete Class");
            btnLeaveOrDelete.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            getResources().getColor(R.color.error_red, null)));
            btnLeaveOrDelete.setOnClickListener(v -> confirmDeleteClass());
        } else {
            btnLeaveOrDelete.setText("Leave Class");
            btnLeaveOrDelete.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            getResources().getColor(R.color.error_red, null)));
            btnLeaveOrDelete.setOnClickListener(v -> confirmLeaveClass());
        }
    }

    private void loadClassCode() {
        apiService.getClassCode(classId)
                .enqueue(new Callback<ApiResponse<String>>() {
                    public void onResponse(Call<ApiResponse<String>> call,
                                           Response<ApiResponse<String>> r) {
                        if (r.isSuccessful() && r.body() != null && r.body().getResult() != null) {
                            runOnUiThread(() -> tvClassCode.setText(r.body().getResult()));
                        }
                    }
                    public void onFailure(Call<ApiResponse<String>> call, Throwable t) {}
                });
    }

    private void confirmDeleteClass() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Class")
                .setMessage("This will permanently delete the class and all its data. Continue?")
                .setPositiveButton("Delete", (d, w) ->
                        apiService.deleteClass(classId).enqueue(new Callback<ApiResponse<Void>>() {
                            public void onResponse(Call<ApiResponse<Void>> call,
                                                   Response<ApiResponse<Void>> r) {
                                if (r.isSuccessful()) {
                                    Toast.makeText(ClassSettingActivity.this,
                                            "Class deleted", Toast.LENGTH_SHORT).show();
                                    Intent i = new Intent(ClassSettingActivity.this,
                                            MyClassesActivity.class);
                                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(i);
                                } else {
                                    Toast.makeText(ClassSettingActivity.this,
                                            "Error " + r.code(), Toast.LENGTH_SHORT).show();
                                }
                            }
                            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                                Toast.makeText(ClassSettingActivity.this,
                                        "Network error", Toast.LENGTH_SHORT).show();
                            }
                        }))
                .setNegativeButton("Cancel", null).show();
    }

    private void confirmLeaveClass() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Class")
                .setMessage("Are you sure you want to leave this class?")
                .setPositiveButton("Leave", (d, w) ->
                        apiService.leaveClass(classId).enqueue(new Callback<ApiResponse<Void>>() {
                            public void onResponse(Call<ApiResponse<Void>> call,
                                                   Response<ApiResponse<Void>> r) {
                                if (r.isSuccessful()) {
                                    Toast.makeText(ClassSettingActivity.this,
                                            "Left class", Toast.LENGTH_SHORT).show();
                                    Intent i = new Intent(ClassSettingActivity.this,
                                            MyClassesActivity.class);
                                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(i);
                                } else {
                                    Toast.makeText(ClassSettingActivity.this,
                                            "Error " + r.code(), Toast.LENGTH_SHORT).show();
                                }
                            }
                            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                                Toast.makeText(ClassSettingActivity.this,
                                        "Network error", Toast.LENGTH_SHORT).show();
                            }
                        }))
                .setNegativeButton("Cancel", null).show();
    }
}