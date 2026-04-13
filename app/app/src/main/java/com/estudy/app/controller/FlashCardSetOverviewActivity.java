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
import com.estudy.app.model.request.CommentRequest;
import com.estudy.app.model.response.ApiResponse;
import com.estudy.app.model.response.CommentResponse;
import com.estudy.app.model.response.FlashCardSetDetailResponse;
import com.estudy.app.utils.BottomNavHelper;
import com.estudy.app.utils.TokenManager;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Màn hình tổng quan bộ flashcard (của bạn Vân).
 * Hiển thị tên, mô tả, số từ, comments.
 * Nhấn vào box Flashcards → sang FlashCardSetDetailActivity (danh sách từ chi tiết).
 * Nhấn nút + → sang FlashCardListActivity (thêm/sửa từ).
 */
public class FlashCardSetOverviewActivity extends AppCompatActivity {

    private TextView tvName, tvDescription, tvPrivacy, tvFlashCardCount;
    private RecyclerView rvComments;
    private LinearLayout layoutFlashCards, layoutReviewHeader, layoutAddComment;
    private ImageButton btnToggleComments, btnSendComment, btnAddCard;
    private EditText etComment;

    private ApiService apiService;
    private TokenManager tokenManager;
    private String flashCardSetId;
    private String flashCardSetName;
    private String flashCardSetOwnerUsername = "";
    private boolean isCommentsVisible = true;
    private boolean isPublic = false;
    private CommentAdapter commentAdapter;
    private List<CommentResponse> commentList = new ArrayList<>();

    // BỘ THU TÍN HIỆU TỪ MÀN HÌNH DANH SÁCH THẺ (THÊM/XÓA/SỬA)
    private final androidx.activity.result.ActivityResultLauncher<Intent> cardListLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // 1. Tải lại API để tự update số cards (từ 2 lên 3) trên màn hình này
                    loadDetail();

                    // 2. Truyền tiếp lệnh báo thành công ra ngoài màn hình Trang chủ (SetListActivity)
                    setResult(RESULT_OK);
                }
            });

    @Override
    protected void onResume() {
        super.onResume();
        loadDetail();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard_set_overview);

        tokenManager  = new TokenManager(this);
        apiService    = ApiClient.getInstance(tokenManager).create(ApiService.class);
        flashCardSetId   = getIntent().getStringExtra("flashcard_set_id");
        flashCardSetName = getIntent().getStringExtra("flashcard_set_name");

        // Toolbar
        View toolbar = findViewById(R.id.toolbar);
        ImageButton btnBack  = toolbar.findViewById(R.id.btnBack);
        TextView    tvTitle  = toolbar.findViewById(R.id.tvToolbarTitle);
        tvTitle.setText(flashCardSetName != null ? flashCardSetName : "Detail");
        btnBack.setOnClickListener(v -> finish());

        // Bind views
        tvName            = findViewById(R.id.tvName);
        tvDescription     = findViewById(R.id.tvDescription);
        tvPrivacy         = findViewById(R.id.tvPrivacy);
        tvFlashCardCount  = findViewById(R.id.tvFlashCardCount);
        rvComments        = findViewById(R.id.rvComments);
        layoutFlashCards  = findViewById(R.id.layoutFlashCards);
        layoutReviewHeader= findViewById(R.id.layoutReviewHeader);
        layoutAddComment  = findViewById(R.id.layoutAddComment);
        btnToggleComments = findViewById(R.id.btnToggleComments);
        btnSendComment    = findViewById(R.id.btnSendComment);
        btnAddCard        = findViewById(R.id.btnAddCard);

        etComment         = findViewById(R.id.etComment);

        rvComments.setLayoutManager(new LinearLayoutManager(this));

        String currentSetId = getIntent().getStringExtra("flashcard_set_id");


        layoutFlashCards.setOnClickListener(v -> {
            Intent intent = new Intent(FlashCardSetOverviewActivity.this, FlashCardListActivity.class);
            intent.putExtra("flashcard_set_id", currentSetId); // Truyền ID của Set
            intent.putExtra("is_edit_mode", false);            // Chế độ chỉ xem
            cardListLauncher.launch(intent);
        });

        btnAddCard.setOnClickListener(v -> {
            Intent intent = new Intent(FlashCardSetOverviewActivity.this, FlashCardListActivity.class);
            intent.putExtra("flashcard_set_id", currentSetId); // Truyền ID của Set
            intent.putExtra("is_edit_mode", true);             // Bật chế độ thêm/sửa
            cardListLauncher.launch(intent);
        });

        btnToggleComments.setOnClickListener(v -> toggleComments());
        btnSendComment.setOnClickListener(v -> handleSendComment());

        BottomNavHelper.setup(this, R.id.btnNavSets);
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
        if (etComment == null) return;
        String content = etComment.getText().toString().trim();
        if (content.isEmpty()) { etComment.setError("Please enter a comment"); return; }
        btnSendComment.setEnabled(false);
        apiService.addComment(flashCardSetId, new CommentRequest(content))
                .enqueue(new Callback<ApiResponse<CommentResponse>>() {
                    @Override public void onResponse(Call<ApiResponse<CommentResponse>> call,
                                                     Response<ApiResponse<CommentResponse>> res) {
                        btnSendComment.setEnabled(true);
                        if (res.isSuccessful() && res.body() != null
                                && res.body().getResult() != null) {
                            commentList.add(res.body().getResult());
                            commentAdapter.notifyItemInserted(commentList.size() - 1);
                            rvComments.scrollToPosition(commentList.size() - 1);
                            etComment.setText("");
                            if (!isCommentsVisible) toggleComments();
                        } else {
                            Toast.makeText(FlashCardSetOverviewActivity.this,
                                    "Comment failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onFailure(Call<ApiResponse<CommentResponse>> call, Throwable t) {
                        btnSendComment.setEnabled(true);
                        Toast.makeText(FlashCardSetOverviewActivity.this,
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
                                    @Override public void onResponse(Call<ApiResponse<Void>> call,
                                                                     Response<ApiResponse<Void>> res) {
                                        if (res.isSuccessful()) commentAdapter.removeItem(item);
                                    }
                                    @Override public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {}
                                }))
                .setNegativeButton("Cancel", null).show();
    }

    private void loadDetail() {
        apiService.getFlashCardSetDetail(flashCardSetId)
                .enqueue(new Callback<ApiResponse<FlashCardSetDetailResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<FlashCardSetDetailResponse>> call,
                                           Response<ApiResponse<FlashCardSetDetailResponse>> response) {
                        if (!response.isSuccessful() || response.body() == null
                                || response.body().getResult() == null) return;

                        FlashCardSetDetailResponse data = response.body().getResult();

                        if (tvName != null)
                            tvName.setText("Name: " + data.getName());
                        if (tvDescription != null)
                            tvDescription.setText("Description: "
                                    + (data.getDescription() != null ? data.getDescription() : "-"));
                        if (tvPrivacy != null)
                            tvPrivacy.setText("Privacy: "
                                    + (data.getPrivacy() != null ? data.getPrivacy() : "-"));

                        int cardCount = data.getFlashCards() != null
                                ? data.getFlashCards().size() : 0;
                        if (tvFlashCardCount != null)
                            tvFlashCardCount.setText(cardCount + " cards");

                        isPublic = "PUBLIC".equals(data.getPrivacy());
                        String ownerUsername = data.getOwnerUsername();
                        String currentUsername = tokenManager.getCurrentUsername();
                        boolean isOwner = currentUsername != null
                                && currentUsername.equals(ownerUsername);

                        // Hiện comment box khi:
                        // - Bộ PUBLIC: tất cả mọi người
                        // - Bộ PRIVATE: chỉ chủ sở hữu
                        boolean canComment = isPublic || isOwner;
                        if (layoutAddComment != null)
                            layoutAddComment.setVisibility(canComment ? View.VISIBLE : View.GONE);

                        commentList = data.getComments() != null
                                ? new ArrayList<>(data.getComments()) : new ArrayList<>();
                        commentAdapter = new CommentAdapter(
                                commentList, currentUsername, flashCardSetOwnerUsername,
                                item -> handleDeleteComment(item));
                        if (rvComments != null) rvComments.setAdapter(commentAdapter);
                        if (btnToggleComments != null)
                            btnToggleComments.setVisibility(
                                    commentList.isEmpty() ? View.GONE : View.VISIBLE);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<FlashCardSetDetailResponse>> call, Throwable t) {
                        Toast.makeText(FlashCardSetOverviewActivity.this,
                                "Load failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}