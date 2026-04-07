package com.estudy.app.controller;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.estudy.app.R;
import com.estudy.app.api.ApiClient;
import com.estudy.app.api.ApiService;
import com.estudy.app.model.request.CommentRequest;
import com.estudy.app.model.response.*;
import com.estudy.app.utils.*;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FlashCardSetDetailActivity extends AppCompatActivity {

    // Views
    private TextView tvName, tvDescription, tvPrivacy, tvFlashCardCount;
    private TextView tvRememberedCount, tvNeedReviewCount, tvNotStudiedCount;
    private StudyProgressView studyProgressView;
    private RecyclerView rvFlashCards, rvComments;
    private LinearLayout layoutAddComment;
    private ImageButton btnToggleComments, btnSendComment, btnMore, btnFilter, btnStudy;
    private EditText etComment, etSearch;

    private ApiService apiService;
    private TokenManager tokenManager;
    private String flashCardSetId;
    private boolean isCommentsVisible = true;
    private boolean isPublic = false;
    private CommentAdapter commentAdapter;
    private List<CommentResponse> commentList = new ArrayList<>();

    // Vocabulary data
    private List<FlashCardResponse> allCards = new ArrayList<>();
    private List<FlashCardResponse> filteredCards = new ArrayList<>();
    private FlashCardDetailAdapter cardAdapter;
    private String currentFilter = "all"; // all | remembered | need_review | not_study

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard_set_detail);

        tokenManager = new TokenManager(this);
        apiService   = ApiClient.getInstance(tokenManager).create(ApiService.class);
        flashCardSetId = getIntent().getStringExtra("flashcard_set_id");
        String name    = getIntent().getStringExtra("flashcard_set_name");

        setupToolbar(name);
        bindViews();
        setupSearch();
        setupButtons();
        BottomNavHelper.setup(this, R.id.btnNavSets);
        loadDetail();
    }

    private void setupToolbar(String name) {
        TextView tvTitle = findViewById(R.id.tvToolbarTitle);
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (tvTitle != null) tvTitle.setText(name != null ? name : "Detail");
        if (btnBack != null) btnBack.setOnClickListener(v -> NavHelper.back(this));
    }

    private void bindViews() {
        tvName             = findViewById(R.id.tvName);
        tvDescription      = findViewById(R.id.tvDescription);
        tvPrivacy          = findViewById(R.id.tvPrivacy);
        tvFlashCardCount   = findViewById(R.id.tvFlashCardCount);
        tvRememberedCount  = findViewById(R.id.tvRememberedCount);
        tvNeedReviewCount  = findViewById(R.id.tvNeedReviewCount);
        tvNotStudiedCount  = findViewById(R.id.tvNotStudiedCount);
        studyProgressView  = findViewById(R.id.studyProgressView);
        rvFlashCards       = findViewById(R.id.rvFlashCards);
        rvComments         = findViewById(R.id.rvComments);
        layoutAddComment   = findViewById(R.id.layoutAddComment);
        btnToggleComments  = findViewById(R.id.btnToggleComments);
        btnSendComment     = findViewById(R.id.btnSendComment);
        btnMore            = findViewById(R.id.btnMore);
        btnFilter          = findViewById(R.id.btnFilter);
        btnStudy           = findViewById(R.id.btnStudy);
        etComment          = findViewById(R.id.etComment);
        etSearch           = findViewById(R.id.etSearch);

        rvFlashCards.setLayoutManager(new LinearLayoutManager(this));
        rvFlashCards.setNestedScrollingEnabled(false);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setNestedScrollingEnabled(false);

        // Adapter vocabulary
        cardAdapter = new FlashCardDetailAdapter(filteredCards, item -> showDotMenu(item));
        rvFlashCards.setAdapter(cardAdapter);
    }

    private void setupSearch() {
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {
                applyFilter(currentFilter, s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupButtons() {
        if (btnFilter != null) btnFilter.setOnClickListener(v -> showFilterSheet());
        if (btnStudy  != null) btnStudy.setOnClickListener(v ->
                StudyModeBottomSheet.newInstance(flashCardSetId,
                                getIntent().getStringExtra("flashcard_set_name"))
                        .show(getSupportFragmentManager(), "StudyMode"));
        if (btnMore   != null) btnMore.setOnClickListener(v -> showMoreMenu());
        if (btnToggleComments != null)
            btnToggleComments.setOnClickListener(v -> toggleComments());
        if (btnSendComment != null)
            btnSendComment.setOnClickListener(v -> handleSendComment());
    }

    // ── Filter bottom sheet ────────────────────────────────────
    private void showFilterSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View v = LayoutInflater.from(this)
                .inflate(R.layout.fragment_filter_bottom_sheet, null);
        dialog.setContentView(v);

        TextView optAll          = v.findViewById(R.id.optAll);
        TextView optRemembered   = v.findViewById(R.id.optRemembered);
        TextView optNeedReview   = v.findViewById(R.id.optNeedReview);
        TextView optNotStudy     = v.findViewById(R.id.optNotStudy);
        ImageButton btnClose     = v.findViewById(R.id.btnCloseFilter);

        if (btnClose != null) btnClose.setOnClickListener(x -> dialog.dismiss());

        View[] opts = {optAll, optRemembered, optNeedReview, optNotStudy};
        String[] keys = {"all", "remembered", "need_review", "not_study"};

        for (int i = 0; i < opts.length; i++) {
            if (opts[i] == null) continue;
            final String key = keys[i];
            opts[i].setBackgroundResource(
                    key.equals(currentFilter) ? R.drawable.bg_mode_selected
                            : android.R.color.transparent);
            opts[i].setOnClickListener(x -> {
                currentFilter = key;
                applyFilter(key, etSearch != null ? etSearch.getText().toString() : "");
                dialog.dismiss();
            });
        }
        dialog.show();
    }

    // ── Apply filter + search ──────────────────────────────────
    private void applyFilter(String filter, String query) {
        filteredCards.clear();
        String q = query != null ? query.toLowerCase().trim() : "";

        for (FlashCardResponse c : allCards) {
            // Search match
            boolean matchQuery = q.isEmpty()
                    || (c.getTerm() != null && c.getTerm().toLowerCase().contains(q))
                    || (c.getDefinition() != null && c.getDefinition().toLowerCase().contains(q));

            // Filter match — khi chưa có StudyRecord, mặc định all pass
            boolean matchFilter = filter.equals("all") || filter.equals("not_study");

            if (matchQuery && matchFilter) filteredCards.add(c);
        }
        cardAdapter.notifyDataSetChanged();
    }

    // ── Horizontal ••• trên từng flashcard: Edit + Delete ─────
    private void showDotMenu(FlashCardResponse item) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View v = LayoutInflater.from(this)
                .inflate(R.layout.fragment_card_menu_sheet, null);
        dialog.setContentView(v);

        View btnEdit   = v.findViewById(R.id.btnEdit);
        View btnDelete = v.findViewById(R.id.btnDelete);

        if (btnEdit != null) btnEdit.setOnClickListener(x -> {
            dialog.dismiss();
            Toast.makeText(this, "Edit " + item.getTerm(), Toast.LENGTH_SHORT).show();
            // TODO: mở FlashCardEditActivity
        });
        if (btnDelete != null) btnDelete.setOnClickListener(x -> {
            dialog.dismiss();
            Toast.makeText(this, "Delete " + item.getTerm(), Toast.LENGTH_SHORT).show();
            // TODO: gọi API xóa flashcard
        });
        dialog.show();
    }

    // ── Vertical ⋮ trên toolbar: Save, Download, Share, Duplicate, AutoPlay, Edit, Delete ──
    private void showMoreMenu() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View v = LayoutInflater.from(this)
                .inflate(R.layout.fragment_set_menu_sheet, null);
        dialog.setContentView(v);

        View btnSave      = v.findViewById(R.id.btnSave);
        View btnDownload  = v.findViewById(R.id.btnDownload);
        View btnShareLink = v.findViewById(R.id.btnShareLink);
        View btnDuplicate = v.findViewById(R.id.btnDuplicate);
        View btnAutoPlay  = v.findViewById(R.id.btnAutoPlay);
        View btnEdit      = v.findViewById(R.id.btnEdit);
        View btnDelete    = v.findViewById(R.id.btnDelete);

        if (btnSave      != null) btnSave.setOnClickListener(x -> {
            dialog.dismiss();
            Toast.makeText(this, "Saved to library", Toast.LENGTH_SHORT).show();
        });
        if (btnDownload  != null) btnDownload.setOnClickListener(x -> {
            dialog.dismiss();
            Toast.makeText(this, "Download — coming soon", Toast.LENGTH_SHORT).show();
        });
        if (btnShareLink != null) btnShareLink.setOnClickListener(x -> {
            dialog.dismiss();
            android.content.ClipboardManager cm =
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(
                    android.content.ClipData.newPlainText("link",
                            "estudy://set/" + flashCardSetId));
            Toast.makeText(this, "Link copied!", Toast.LENGTH_SHORT).show();
        });
        if (btnDuplicate != null) btnDuplicate.setOnClickListener(x -> {
            dialog.dismiss();
            Toast.makeText(this, "Duplicate — coming soon", Toast.LENGTH_SHORT).show();
        });
        if (btnAutoPlay  != null) btnAutoPlay.setOnClickListener(x -> {
            dialog.dismiss();
            Toast.makeText(this, "Auto play — coming soon", Toast.LENGTH_SHORT).show();
        });
        if (btnEdit != null) btnEdit.setOnClickListener(x -> {
            dialog.dismiss();
            Intent intent = new Intent(this, FlashCardSetEditActivity.class);
            intent.putExtra("flashcard_set_id", flashCardSetId);
            intent.putExtra("flashcard_set_name",
                    getIntent().getStringExtra("flashcard_set_name"));
            NavHelper.go(this, intent);
        });
        if (btnDelete != null) btnDelete.setOnClickListener(x -> {
            dialog.dismiss();
            Toast.makeText(this, "Delete set — coming soon", Toast.LENGTH_SHORT).show();
        });
        dialog.show();
    }

    // ── Load API ───────────────────────────────────────────────
    private void loadDetail() {
        apiService.getFlashCardSetDetail(flashCardSetId)
                .enqueue(new Callback<ApiResponse<FlashCardSetDetailResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<FlashCardSetDetailResponse>> call,
                                           Response<ApiResponse<FlashCardSetDetailResponse>> res) {
                        if (!res.isSuccessful() || res.body() == null
                                || res.body().getResult() == null) return;

                        FlashCardSetDetailResponse data = res.body().getResult();

                        // Vocabulary
                        allCards.clear();
                        if (data.getFlashCards() != null) allCards.addAll(data.getFlashCards());
                        applyFilter(currentFilter, "");

                        // Progress: chưa có StudyRecord API → tất cả là "not studied"
                        int total = allCards.size();
                        updateProgress(0, 0, total);

                        // Privacy
                        isPublic = "PUBLIC".equals(data.getPrivacy());
                        if (layoutAddComment != null)
                            layoutAddComment.setVisibility(isPublic ? View.VISIBLE : View.GONE);

                        // Comments
                        String currentUser = tokenManager.getCurrentUsername();
                        commentList = data.getComments() != null
                                ? new ArrayList<>(data.getComments()) : new ArrayList<>();
                        commentAdapter = new CommentAdapter(
                                commentList, currentUser, "",
                                item -> handleDeleteComment(item));
                        if (rvComments != null) rvComments.setAdapter(commentAdapter);
                        if (btnToggleComments != null)
                            btnToggleComments.setVisibility(
                                    commentList.isEmpty() ? View.GONE : View.VISIBLE);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<FlashCardSetDetailResponse>> call,
                                          Throwable t) {
                        Toast.makeText(FlashCardSetDetailActivity.this,
                                "Load failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateProgress(int remembered, int needReview, int notStudied) {
        if (studyProgressView != null)
            studyProgressView.setData(remembered, needReview, notStudied);
        if (tvRememberedCount != null) tvRememberedCount.setText(String.valueOf(remembered));
        if (tvNeedReviewCount != null) tvNeedReviewCount.setText(String.valueOf(needReview));
        if (tvNotStudiedCount != null) tvNotStudiedCount.setText(String.valueOf(notStudied));
    }

    private void toggleComments() {
        isCommentsVisible = !isCommentsVisible;
        if (rvComments != null)
            rvComments.setVisibility(isCommentsVisible ? View.VISIBLE : View.GONE);
        if (btnToggleComments != null)
            btnToggleComments.setImageResource(isCommentsVisible
                    ? android.R.drawable.arrow_up_float
                    : android.R.drawable.arrow_down_float);
    }

    private void handleSendComment() {
        if (etComment == null) return;
        String content = etComment.getText().toString().trim();
        if (content.isEmpty()) return;
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
                            etComment.setText("");
                            if (!isCommentsVisible) toggleComments();
                        }
                    }
                    @Override public void onFailure(Call<ApiResponse<CommentResponse>> call,
                                                    Throwable t) {
                        btnSendComment.setEnabled(true);
                    }
                });
    }

    private void handleDeleteComment(CommentResponse item) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete comment")
                .setPositiveButton("Delete", (d, w) ->
                        apiService.deleteComment(item.getId())
                                .enqueue(new Callback<ApiResponse<Void>>() {
                                    @Override public void onResponse(
                                            Call<ApiResponse<Void>> call,
                                            Response<ApiResponse<Void>> res) {
                                        if (res.isSuccessful()) commentAdapter.removeItem(item);
                                    }
                                    @Override public void onFailure(
                                            Call<ApiResponse<Void>> call, Throwable t) {}
                                }))
                .setNegativeButton("Cancel", null).show();
    }
}