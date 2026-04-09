package com.estudy.app.controller;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.estudy.app.R;
import com.estudy.app.api.ApiClient;
import com.estudy.app.api.ApiService;
import com.estudy.app.model.request.ClassRequest;
import com.estudy.app.model.response.ApiResponse;
import com.estudy.app.model.response.ClassResponse;
import com.estudy.app.utils.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateClassActivity extends AppCompatActivity {

    private EditText etName, etDescription;
    private Spinner spinnerPrivacy;
    private Button btnCreate;
    private ApiService apiService;
    private TokenManager tokenManager;

    private boolean isEdit = false;
    private String editClassId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_class);

        tokenManager = new TokenManager(this);
        apiService = ApiClient.getInstance(tokenManager).create(ApiService.class);

        View toolbar = findViewById(R.id.toolbar);
        ImageButton btnBack = toolbar.findViewById(R.id.btnBack);
        TextView tvTitle = toolbar.findViewById(R.id.tvToolbarTitle);

        etName = findViewById(R.id.etName);
        etDescription = findViewById(R.id.etDescription);
        spinnerPrivacy = findViewById(R.id.spinnerPrivacy);
        btnCreate = findViewById(R.id.btnCreate);

        ArrayAdapter<String> privacyAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                new String[]{"Private", "Public"});
        privacyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPrivacy.setAdapter(privacyAdapter);

        isEdit = getIntent().getBooleanExtra("edit", false);
        if (isEdit) {
            tvTitle.setText("Edit Class");
            btnCreate.setText("Save");
            editClassId = getIntent().getStringExtra("classId");
            etName.setText(getIntent().getStringExtra("name"));
            etDescription.setText(getIntent().getStringExtra("description"));
            String privacy = getIntent().getStringExtra("privacy");
            spinnerPrivacy.setSelection("PUBLIC".equals(privacy) ? 1 : 0);
        } else {
            tvTitle.setText("Create a new class");
        }

        btnBack.setOnClickListener(v -> finish());
        btnCreate.setOnClickListener(v -> submitForm());
    }

    private void submitForm() {
        String name = etName.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        String privacy = spinnerPrivacy.getSelectedItemPosition() == 1 ? "PUBLIC" : "PRIVATE";

        if (name.isEmpty()) {
            etName.setError("Name is required");
            return;
        }

        ClassRequest req = new ClassRequest(name, desc, privacy);

        if (isEdit) {
            apiService.updateClass(editClassId, req).enqueue(new Callback<ApiResponse<ClassResponse>>() {
                public void onResponse(Call<ApiResponse<ClassResponse>> call,
                                       Response<ApiResponse<ClassResponse>> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(CreateClassActivity.this, "Updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(CreateClassActivity.this, "Update failed: " + response.code(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
                public void onFailure(Call<ApiResponse<ClassResponse>> call, Throwable t) {
                    Toast.makeText(CreateClassActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            apiService.createClass(req).enqueue(new Callback<ApiResponse<ClassResponse>>() {
                public void onResponse(Call<ApiResponse<ClassResponse>> call,
                                       Response<ApiResponse<ClassResponse>> response) {
                    if (response.isSuccessful() && response.body() != null
                            && response.body().getResult() != null) {
                        Toast.makeText(CreateClassActivity.this, "Class created!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(CreateClassActivity.this, "Create failed: " + response.code(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
                public void onFailure(Call<ApiResponse<ClassResponse>> call, Throwable t) {
                    Toast.makeText(CreateClassActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}