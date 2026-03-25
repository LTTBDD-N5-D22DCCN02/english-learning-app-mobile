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
import com.estudy.app.model.request.CommentRequest;
import com.estudy.app.model.response.ApiResponse;
import com.estudy.app.model.response.CommentResponse;
import com.estudy.app.model.response.FlashCardSetDetailResponse;
import com.estudy.app.utils.TokenManager;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FlashCardSetDetailActivity extends AppCompatActivity {

    private TextView tvName, tvDescription, tvPrivacy, tvFlashCardCount;
    private RecyclerView rvComments;
    private LinearLayout layoutFlashCards, layoutReviewHeader, layoutAddComment;
    private ImageButton btnToggleComments, btnSendComment;
    private EditText etComment;
    private ApiService apiService;
    private TokenManager tokenManager;
    private String flashCardSetId;
    private String flashCardSetOwnerUsername = "";
    private boolean isCommentsVisible = true;
    private boolean isPublic = false;
    private CommentAdapter commentAdapter;
    private List<CommentResponse> commentList = new ArrayList<>();

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
        layoutAddComment = findViewById(R.id.layoutAddComment);
        btnToggleComments = findViewById(R.id.btnToggleComments);
        btnSendComment = findViewById(R.id.btnSendComment);
        etComment = findViewById(R.id.etComment);

        rvComments.setLayoutManager(new LinearLayoutManager(this));

        flashCardSetId = getIntent().getStringExtra("flashcard_set_id");

        layoutFlashCards.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(
                    this, FlashCardListActivity.class);
            intent.putExtra("flashcard_set_id", flashCardSetId);
            startActivity(intent);
        });

        btnToggleComments.setOnClickListener(v -> toggleComments());

        btnSendComment.setOnClickListener(v -> handleSendComment());

        loadDetail();
    }

    private void toggleComments() {
        isCommentsVisible = !isCommentsVisible;
        rvComments.setVisibility(isCommentsVisible ? View.VISIBLE : View.GONE);
        btnToggleComments.setImageResource(isCommentsVisible
                ? android.R.drawable.arrow_up_float
                : android.R.drawable.arrow_down_float);
    }

    private void handleSendComment() {
        String content = etComment.getText().toString().trim();
        if (content.isEmpty()) {
            etComment.setError("Please enter a comment");
            return;
        }

        btnSendComment.setEnabled(false);

        apiService.addComment(flashCardSetId, new CommentRequest(content))
                .enqueue(new Callback<ApiResponse<CommentResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<CommentResponse>> call,
                                           Response<ApiResponse<CommentResponse>> response) {
                        btnSendComment.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResult() != null) {
                            // Thêm comment mới vào danh sách
                            commentList.add(response.body().getResult());
                            commentAdapter.notifyItemInserted(commentList.size() - 1);
                            rvComments.scrollToPosition(commentList.size() - 1);
                            etComment.setText("");

                            // Hiện lại list nếu đang ẩn
                            if (!isCommentsVisible) toggleComments();
                        } else {
                            Toast.makeText(FlashCardSetDetailActivity.this,
                                    "Comment failed", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<CommentResponse>> call, Throwable t) {
                        btnSendComment.setEnabled(true);
                        Toast.makeText(FlashCardSetDetailActivity.this,
                                "Connection error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleDeleteComment(CommentResponse item) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete comment")
                .setMessage("Are you sure you want to delete this comment?")
                .setPositiveButton("Delete", (dialog, which) ->
                        apiService.deleteComment(item.getId())
                                .enqueue(new Callback<ApiResponse<Void>>() {
                                    @Override
                                    public void onResponse(Call<ApiResponse<Void>> call,
                                                           Response<ApiResponse<Void>> response) {
                                        if (response.isSuccessful()) {
                                            commentAdapter.removeItem(item);
                                            Toast.makeText(FlashCardSetDetailActivity.this,
                                                    "Deleted", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<ApiResponse<Void>> call,
                                                          Throwable t) {
                                        Toast.makeText(FlashCardSetDetailActivity.this,
                                                "Delete failed", Toast.LENGTH_SHORT).show();
                                    }
                                }))
                .setNegativeButton("Cancel", null)
                .show();
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

                            // Kiểm tra privacy
                            isPublic = "PUBLIC".equals(data.getPrivacy());
                            layoutAddComment.setVisibility(isPublic ? View.VISIBLE : View.GONE);

                            // Lấy username hiện tại từ JWT
                            String currentUsername = tokenManager.getCurrentUsername();

                            // Setup comments
                            commentList = data.getComments() != null
                                    ? new ArrayList<>(data.getComments())
                                    : new ArrayList<>();

                            commentAdapter = new CommentAdapter(
                                    commentList,
                                    currentUsername,
                                    flashCardSetOwnerUsername,
                                    item -> handleDeleteComment(item)
                            );
                            rvComments.setAdapter(commentAdapter);

                            if (commentList.isEmpty()) {
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