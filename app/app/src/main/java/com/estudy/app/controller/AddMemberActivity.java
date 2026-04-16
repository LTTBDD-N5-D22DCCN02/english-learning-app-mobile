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
import com.estudy.app.utils.TokenManager;

public class AddMemberActivity extends AppCompatActivity {

    private String classId;
    private ApiService apiService;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_member);

        classId = getIntent().getStringExtra("classId");
        tokenManager = new TokenManager(this);
        apiService = ApiClient.getInstance(tokenManager).create(ApiService.class);

        View toolbar = findViewById(R.id.toolbar);
        ImageButton btnBack = toolbar.findViewById(R.id.btnBack);
        TextView tvTitle = toolbar.findViewById(R.id.tvToolbarTitle);
        tvTitle.setText("Add member");
        btnBack.setOnClickListener(v -> finish());

        Button btnApprove = findViewById(R.id.btnApproveJoinRequest);
        Button btnClassCode = findViewById(R.id.btnClassCode);

        btnApprove.setOnClickListener(v -> {
            Intent i = new Intent(this, ApproveRequestsActivity.class);
            i.putExtra("classId", classId);
            startActivity(i);
        });

        btnClassCode.setOnClickListener(v -> showClassCode());
    }

    private void showClassCode() {
        apiService.getClassCode(classId).enqueue(new retrofit2.Callback<com.estudy.app.model.response.ApiResponse<String>>() {
            public void onResponse(retrofit2.Call<com.estudy.app.model.response.ApiResponse<String>> call,
                                   retrofit2.Response<com.estudy.app.model.response.ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getResult() != null) {
                    String code = response.body().getResult();
                    runOnUiThread(() -> {
                        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(AddMemberActivity.this);
                        builder.setTitle("Class code:");
                        TextView tv = new TextView(AddMemberActivity.this);
                        tv.setText(code);
                        tv.setPadding(40, 30, 40, 20);
                        tv.setTextSize(22f);
                        tv.setGravity(android.view.Gravity.CENTER);
                        builder.setView(tv);
                        builder.setPositiveButton("Copy", (d, w) -> {
                            android.content.ClipboardManager cm = (android.content.ClipboardManager)
                                    getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("class_code", code));
                            Toast.makeText(AddMemberActivity.this, "Copied!", Toast.LENGTH_SHORT).show();
                        });
                        builder.setNegativeButton("Cancel", null);
                        builder.show();
                    });
                }
            }
            public void onFailure(retrofit2.Call<com.estudy.app.model.response.ApiResponse<String>> call, Throwable t) {
                Toast.makeText(AddMemberActivity.this, "Error loading code", Toast.LENGTH_SHORT).show();
            }
        });
    }
}