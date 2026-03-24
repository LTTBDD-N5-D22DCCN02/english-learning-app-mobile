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
import com.estudy.app.utils.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FlashCardSetDetailActivity extends AppCompatActivity {

    private TextView tvName, tvDescription, tvPrivacy, tvFlashCardCount;
    private RecyclerView rvComments;
    private LinearLayout layoutFlashCards, layoutReviewHeader;
    private ImageButton btnToggleComments;
    private ApiService apiService;
    private TokenManager tokenManager;
    private String flashCardSetId;
    private boolean isCommentsVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard_set_detail);

        tokenManager = new TokenManager(this);
        apiService = ApiClient.getInstance(tokenManager).create(ApiService.class);

        // Toolbar
        View toolbar = findViewById(R.id.toolbar);
        ImageButton btnBack = toolbar.findViewById(R.id.btnBack);
        TextView tvTitle = toolbar.findViewById(R.id.tvToolbarTitle);
        String name = getIntent().getStringExtra("flashcard_set_name");
        tvTitle.setText(name != null ? name : "Detail");
        btnBack.setOnClickListener(v -> finish());

        // Views
        tvName = findViewById(R.id.tvName);
        tvDescription = findViewById(R.id.tvDescription);
        tvPrivacy = findViewById(R.id.tvPrivacy);
        tvFlashCardCount = findViewById(R.id.tvFlashCardCount);
        rvComments = findViewById(R.id.rvComments);
        layoutFlashCards = findViewById(R.id.layoutFlashCards);
        layoutReviewHeader = findViewById(R.id.layoutReviewHeader);
        btnToggleComments = findViewById(R.id.btnToggleComments);

        rvComments.setLayoutManager(new LinearLayoutManager(this));

        flashCardSetId = getIntent().getStringExtra("flashcard_set_id");

        // Nhấn vào ô Flashcards → sang màn danh sách flashcard
        layoutFlashCards.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(
                    this, FlashCardListActivity.class);
            intent.putExtra("flashcard_set_id", flashCardSetId);
            startActivity(intent);
        });

        // Toggle ẩn/hiện comments
        btnToggleComments.setOnClickListener(v -> toggleComments());

        loadDetail();
    }

    private void toggleComments() {
        isCommentsVisible = !isCommentsVisible;
        if (isCommentsVisible) {
            rvComments.setVisibility(View.VISIBLE);
            btnToggleComments.setImageResource(android.R.drawable.arrow_up_float);
        } else {
            rvComments.setVisibility(View.GONE);
            btnToggleComments.setImageResource(android.R.drawable.arrow_down_float);
        }
    }

    private void loadDetail() {
        apiService.getFlashCardSetDetail(flashCardSetId)
                .enqueue(new Callback<ApiResponse<FlashCardSetDetailResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<FlashCardSetDetailResponse>> call,
                                           Response<ApiResponse<FlashCardSetDetailResponse>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResult() != null) {

                            FlashCardSetDetailResponse data = response.body().getResult();

                            tvName.setText("Name: " + data.getName());
                            tvDescription.setText("Description: " +
                                    (data.getDescription() != null ? data.getDescription() : "-"));
                            tvPrivacy.setText("Privacy: " +
                                    (data.getPrivacy() != null ? data.getPrivacy() : "-"));

                            int cardCount = data.getFlashCards() != null
                                    ? data.getFlashCards().size() : 0;
                            tvFlashCardCount.setText(cardCount + " cards");

                            if (data.getComments() != null && !data.getComments().isEmpty()) {
                                CommentAdapter commentAdapter =
                                        new CommentAdapter(data.getComments());
                                rvComments.setAdapter(commentAdapter);
                            } else {
                                rvComments.setVisibility(View.GONE);
                                btnToggleComments.setVisibility(View.GONE);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<FlashCardSetDetailResponse>> call,
                                          Throwable t) {
                        Toast.makeText(FlashCardSetDetailActivity.this,
                                "Load failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}