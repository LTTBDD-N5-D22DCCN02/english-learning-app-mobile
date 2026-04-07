package com.estudy.app.controller;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.estudy.app.R;
import com.estudy.app.api.ApiClient;
import com.estudy.app.api.ApiService;
import com.estudy.app.model.request.FlashCardSetRequest;
import com.estudy.app.model.response.ApiResponse;
import com.estudy.app.model.response.FlashCardSetResponse;
import com.estudy.app.utils.BottomNavHelper;
import com.estudy.app.utils.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FlashCardSetCreateActivity extends AppCompatActivity {

    private EditText etName, etDescription;
    private Spinner spinnerPrivacy;
    private Button btnSave;
    private ImageButton btnBack;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard_set_create);
        BottomNavHelper.setup(this, R.id.btnNavAdd);

        TokenManager tokenManager = new TokenManager(this);
        apiService = ApiClient.getInstance(tokenManager).create(ApiService.class);

        View toolbar = findViewById(R.id.toolbar);
        ImageButton btnBack = toolbar.findViewById(R.id.btnBack);
        TextView tvTitle = toolbar.findViewById(R.id.tvToolbarTitle);
        tvTitle.setText("Create Flashcard Set");
        btnBack.setOnClickListener(v -> finish());

        etName = findViewById(R.id.etName);
        etDescription = findViewById(R.id.etDescription);
        spinnerPrivacy = findViewById(R.id.spinnerPrivacy);
        btnSave = findViewById(R.id.btnSave);

        // Setup privacy spinner
        ArrayAdapter<String> privacyAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"PUBLIC", "PRIVATE"});
        privacyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPrivacy.setAdapter(privacyAdapter);
        spinnerPrivacy.setSelection(1); // default PRIVATE

        btnSave.setOnClickListener(v -> handleSave());
    }

    private void handleSave() {
        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String privacy = spinnerPrivacy.getSelectedItem().toString();

        if (name.isEmpty()) {
            etName.setError("Name is required");
            return;
        }

        btnSave.setEnabled(false);

        FlashCardSetRequest request = new FlashCardSetRequest(name, description, privacy);

        apiService.createFlashCardSet(request).enqueue(new Callback<ApiResponse<FlashCardSetResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<FlashCardSetResponse>> call,
                                   Response<ApiResponse<FlashCardSetResponse>> response) {
                btnSave.setEnabled(true);
                if (response.isSuccessful() && response.body() != null
                        && response.body().getResult() != null) {
                    Toast.makeText(FlashCardSetCreateActivity.this,
                            "Created successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(FlashCardSetCreateActivity.this,
                            "Create failed. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<FlashCardSetResponse>> call, Throwable t) {
                btnSave.setEnabled(true);
                Toast.makeText(FlashCardSetCreateActivity.this,
                        "Connection error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}