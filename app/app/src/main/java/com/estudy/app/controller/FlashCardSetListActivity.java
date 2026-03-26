package com.estudy.app.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.estudy.app.R;
import com.estudy.app.api.ApiClient;
import com.estudy.app.api.ApiService;
import com.estudy.app.model.response.ApiResponse;
import com.estudy.app.model.response.FlashCardSetResponse;
import com.estudy.app.utils.TokenManager;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FlashCardSetListActivity extends AppCompatActivity {

    private RecyclerView rvFlashCardSets;
    private ImageButton btnBack, btnAdd;
    private ApiService apiService;
    private TokenManager tokenManager;
    private List<FlashCardSetResponse> flashCardSetList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard_set_list);

        tokenManager = new TokenManager(this);
        apiService = ApiClient.getInstance(tokenManager).create(ApiService.class);

        // Toolbar
        View toolbar = findViewById(R.id.toolbar);
        ImageButton btnBack = toolbar.findViewById(R.id.btnBack);
        TextView tvTitle = toolbar.findViewById(R.id.tvToolbarTitle);
        tvTitle.setText("Flashcard Sets");
        btnBack.setOnClickListener(v -> finish());

        rvFlashCardSets = findViewById(R.id.rvFlashCardSets);
        btnAdd = findViewById(R.id.btnAdd);

        rvFlashCardSets.setLayoutManager(new LinearLayoutManager(this));

        btnAdd.setOnClickListener(v ->
                startActivity(new Intent(this, FlashCardSetCreateActivity.class)));

        loadFlashCardSets();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFlashCardSets();
    }

    private void loadFlashCardSets() {
        apiService.getMyFlashCardSets().enqueue(new Callback<ApiResponse<List<FlashCardSetResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<FlashCardSetResponse>>> call,
                                   Response<ApiResponse<List<FlashCardSetResponse>>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getResult() != null) {
                    flashCardSetList = response.body().getResult();
                    setupAdapter();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<FlashCardSetResponse>>> call, Throwable t) {
                Toast.makeText(FlashCardSetListActivity.this,
                        "Load failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupAdapter() {
        FlashCardSetAdapter adapter = new FlashCardSetAdapter(flashCardSetList,
                // onClick item -> detail
                item -> {
                    Intent intent = new Intent(this, FlashCardSetDetailActivity.class);
                    intent.putExtra("flashcard_set_id", item.getId());
                    intent.putExtra("flashcard_set_name", item.getName());
                    startActivity(intent);
                },
                // onClick edit
                item -> {
                    Intent intent = new Intent(this, FlashCardSetEditActivity.class);
                    intent.putExtra("flashcard_set_id", item.getId());
                    intent.putExtra("flashcard_set_name", item.getName());
                    intent.putExtra("flashcard_set_description", item.getDescription());
                    intent.putExtra("flashcard_set_privacy", item.getPrivacy());
                    startActivity(intent);
                },
                // onClick delete
                item -> deleteFlashCardSet(item.getId()));
        rvFlashCardSets.setAdapter(adapter);
    }

    private void deleteFlashCardSet(String id) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete")
                .setMessage("Are you sure you want to delete this flashcard set?")
                .setPositiveButton("Delete", (dialog, which) ->
                        apiService.deleteFlashCardSet(id).enqueue(new Callback<ApiResponse<Void>>() {
                            @Override
                            public void onResponse(Call<ApiResponse<Void>> call,
                                                   Response<ApiResponse<Void>> response) {
                                Toast.makeText(FlashCardSetListActivity.this,
                                        "Deleted successfully", Toast.LENGTH_SHORT).show();
                                loadFlashCardSets();
                            }

                            @Override
                            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                                Toast.makeText(FlashCardSetListActivity.this,
                                        "Delete failed", Toast.LENGTH_SHORT).show();
                            }
                        }))
                .setNegativeButton("Cancel", null)
                .show();
    }
}