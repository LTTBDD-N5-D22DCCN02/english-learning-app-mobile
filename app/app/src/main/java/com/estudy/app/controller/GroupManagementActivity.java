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
import com.estudy.app.utils.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupManagementActivity extends AppCompatActivity {

    private ApiService apiService;
    private TokenManager tokenManager;
    private String classId, className;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_management);

        classId = getIntent().getStringExtra("classId");
        className = getIntent().getStringExtra("className");

        tokenManager = new TokenManager(this);
        apiService = ApiClient.getInstance(tokenManager).create(ApiService.class);

        View toolbar = findViewById(R.id.toolbar);
        ImageButton btnBack = toolbar.findViewById(R.id.btnBack);
        TextView tvTitle = toolbar.findViewById(R.id.tvToolbarTitle);
        tvTitle.setText("Group Management");
        btnBack.setOnClickListener(v -> finish());

        Button btnAddMember = findViewById(R.id.btnAddMember);
        Button btnDeleteClass = findViewById(R.id.btnDeleteClass);
        Button btnEditClass = findViewById(R.id.btnEditClass);

        btnAddMember.setOnClickListener(v -> {
            Intent i = new Intent(this, AddMemberActivity.class);
            i.putExtra("classId", classId);
            startActivity(i);
        });

        btnDeleteClass.setOnClickListener(v -> confirmDelete());

        btnEditClass.setOnClickListener(v -> {
            // Reload class detail for current info then navigate
            apiService.getClassDetail(classId).enqueue(new Callback<ApiResponse<com.estudy.app.model.response.ClassResponse>>() {
                public void onResponse(Call<ApiResponse<com.estudy.app.model.response.ClassResponse>> call,
                                       Response<ApiResponse<com.estudy.app.model.response.ClassResponse>> r) {
                    if (r.isSuccessful() && r.body() != null && r.body().getResult() != null) {
                        com.estudy.app.model.response.ClassResponse cls = r.body().getResult();
                        Intent i = new Intent(GroupManagementActivity.this, CreateClassActivity.class);
                        i.putExtra("edit", true);
                        i.putExtra("classId", cls.getId());
                        i.putExtra("name", cls.getName());
                        i.putExtra("description", cls.getDescription());
                        i.putExtra("privacy", cls.getPrivacy());
                        startActivity(i);
                    }
                }
                public void onFailure(Call<ApiResponse<com.estudy.app.model.response.ClassResponse>> call, Throwable t) {
                    Toast.makeText(GroupManagementActivity.this, "Error", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Class")
                .setMessage("Are you sure you want to delete this class? All data will be removed.")
                .setPositiveButton("Delete", (d, w) -> {
                    apiService.deleteClass(classId).enqueue(new Callback<ApiResponse<Void>>() {
                        public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> r) {
                            if (r.isSuccessful()) {
                                Toast.makeText(GroupManagementActivity.this, "Class deleted", Toast.LENGTH_SHORT).show();
                                // Go back to My Classes
                                Intent i = new Intent(GroupManagementActivity.this, MyClassesActivity.class);
                                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(i);
                            } else {
                                Toast.makeText(GroupManagementActivity.this, "Error", Toast.LENGTH_SHORT).show();
                            }
                        }
                        public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                            Toast.makeText(GroupManagementActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null).show();
    }
}