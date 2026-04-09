package com.estudy.app.controller;

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

public class ClassDetailActivity extends AppCompatActivity {

    private String classId, className;
    private Button btnGroupManagement;
    private ApiService apiService;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_detail);

        classId   = getIntent().getStringExtra("classId");
        className = getIntent().getStringExtra("className");

        tokenManager = new TokenManager(this);
        apiService   = ApiClient.getInstance(tokenManager).create(ApiService.class);

        View toolbar = findViewById(R.id.toolbar);
        ((ImageButton) toolbar.findViewById(R.id.btnBack)).setOnClickListener(v -> finish());
        ((TextView) toolbar.findViewById(R.id.tvToolbarTitle))
                .setText(className != null ? className : "Class");

        btnGroupManagement = findViewById(R.id.btnGroupManagement);
        Button btnFlashcardSets = findViewById(R.id.btnFlashcardSets);
        Button btnMemberList    = findViewById(R.id.btnMemberList);
        Button btnSetting       = findViewById(R.id.btnSetting);

        btnFlashcardSets.setOnClickListener(v -> openFlashcardSets());
        btnMemberList.setOnClickListener(v -> openMemberList());
        btnGroupManagement.setOnClickListener(v -> openGroupManagement());
        btnSetting.setOnClickListener(v -> openSetting());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Luôn reload role từ server để đảm bảo đúng sau khi chuyển quyền
        reloadRole();
    }

    /** Gọi API lấy lại role thực tế, rồi show/hide Group Management */
    private void reloadRole() {
        apiService.getClassDetail(classId)
                .enqueue(new Callback<ApiResponse<ClassResponse>>() {
                    public void onResponse(Call<ApiResponse<ClassResponse>> call,
                                           Response<ApiResponse<ClassResponse>> r) {
                        if (r.isSuccessful() && r.body() != null && r.body().getResult() != null) {
                            String role = r.body().getResult().getMyRole();
                            runOnUiThread(() -> {
                                boolean canManage = "LEADER".equals(role) || "ADMIN".equals(role);
                                btnGroupManagement.setVisibility(
                                        canManage ? View.VISIBLE : View.GONE);
                            });
                        }
                    }
                    public void onFailure(Call<ApiResponse<ClassResponse>> call, Throwable t) {}
                });
    }

    private void openFlashcardSets() {
        apiService.getClassDetail(classId)
                .enqueue(new Callback<ApiResponse<ClassResponse>>() {
                    public void onResponse(Call<ApiResponse<ClassResponse>> call,
                                           Response<ApiResponse<ClassResponse>> r) {
                        String role = (r.isSuccessful() && r.body() != null
                                && r.body().getResult() != null)
                                ? r.body().getResult().getMyRole() : "";
                        Intent i = new Intent(ClassDetailActivity.this,
                                ClassFlashcardSetsActivity.class);
                        i.putExtra("classId", classId);
                        i.putExtra("className", className);
                        i.putExtra("myRole", role);
                        startActivity(i);
                    }
                    public void onFailure(Call<ApiResponse<ClassResponse>> call, Throwable t) {
                        Toast.makeText(ClassDetailActivity.this, "Error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openMemberList() {
        apiService.getClassDetail(classId)
                .enqueue(new Callback<ApiResponse<ClassResponse>>() {
                    public void onResponse(Call<ApiResponse<ClassResponse>> call,
                                           Response<ApiResponse<ClassResponse>> r) {
                        String role = (r.isSuccessful() && r.body() != null
                                && r.body().getResult() != null)
                                ? r.body().getResult().getMyRole() : "";
                        Intent i = new Intent(ClassDetailActivity.this, MemberListActivity.class);
                        i.putExtra("classId", classId);
                        i.putExtra("className", className);
                        i.putExtra("myRole", role);
                        startActivity(i);
                    }
                    public void onFailure(Call<ApiResponse<ClassResponse>> call, Throwable t) {
                        Toast.makeText(ClassDetailActivity.this, "Error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openGroupManagement() {
        Intent i = new Intent(this, GroupManagementActivity.class);
        i.putExtra("classId", classId);
        i.putExtra("className", className);
        startActivity(i);
    }

    private void openSetting() {
        Intent i = new Intent(this, ClassSettingActivity.class);
        i.putExtra("classId", classId);
        i.putExtra("className", className);
        startActivity(i);
    }
}