package com.estudy.app.controller;

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
import com.estudy.app.model.response.FlashCardSetDetailResponse;
import com.estudy.app.utils.BottomNavHelper;
import com.estudy.app.utils.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FlashCardListActivity extends AppCompatActivity {

    private RecyclerView rvFlashCards;
    private ImageButton btnBack;
    private ApiService apiService;
    private String flashCardSetId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard_list);
        BottomNavHelper.setup(this, R.id.btnNavSets);

        TokenManager tokenManager = new TokenManager(this);
        apiService = ApiClient.getInstance(tokenManager).create(ApiService.class);

        View toolbar = findViewById(R.id.toolbar);
        ImageButton btnBack = toolbar.findViewById(R.id.btnBack);
        TextView tvTitle = toolbar.findViewById(R.id.tvToolbarTitle);
        tvTitle.setText("Flashcards");
        btnBack.setOnClickListener(v -> finish());

        rvFlashCards = findViewById(R.id.rvFlashCards);

        rvFlashCards.setLayoutManager(new LinearLayoutManager(this));

        flashCardSetId = getIntent().getStringExtra("flashcard_set_id");

        loadFlashCards();
    }

    private void loadFlashCards() {
        apiService.getFlashCardSetDetail(flashCardSetId)
                .enqueue(new Callback<ApiResponse<FlashCardSetDetailResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<FlashCardSetDetailResponse>> call,
                                           Response<ApiResponse<FlashCardSetDetailResponse>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResult() != null
                                && response.body().getResult().getFlashCards() != null) {

                            FlashCardAdapter adapter = new FlashCardAdapter(
                                    response.body().getResult().getFlashCards());
                            rvFlashCards.setAdapter(adapter);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<FlashCardSetDetailResponse>> call,
                                          Throwable t) {
                        Toast.makeText(FlashCardListActivity.this,
                                "Load failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}