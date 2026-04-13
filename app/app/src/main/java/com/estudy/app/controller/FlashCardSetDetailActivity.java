package com.estudy.app.controller;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import android.widget.PopupWindow;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.estudy.app.R;
import com.estudy.app.api.ApiClient;
import com.estudy.app.api.ApiService;
import com.estudy.app.model.response.*;
import com.estudy.app.utils.*;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FlashCardSetDetailActivity extends AppCompatActivity {

    // Views
    private TextView tvName, tvDescription, tvPrivacy, tvFlashCardCount;
    private TextView tvRememberedCount, tvNeedReviewCount, tvNotStudiedCount;
    private TextToSpeech tts;
    private StudyProgressView studyProgressView;
    private RecyclerView rvFlashCards;
    private ImageButton btnMore, btnFilter, btnStudy;
    private EditText etSearch;
    private com.google.android.material.button.MaterialButton btnAddFlashcard;

    private ApiService apiService;
    private TokenManager tokenManager;
    private String flashCardSetId;
    private boolean isCommentsVisible = true;
    private boolean isPublic = false;
    private boolean isOwner  = false;

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

        // Khởi tạo TTS để phát âm từng từ trong danh sách
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
            }
        });

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

        btnMore            = findViewById(R.id.btnMore);
        btnFilter          = findViewById(R.id.btnFilter);
        btnStudy           = findViewById(R.id.btnStudy);
        etSearch           = findViewById(R.id.etSearch);
        btnAddFlashcard    = findViewById(R.id.btnAddFlashcard);

        rvFlashCards.setLayoutManager(new LinearLayoutManager(this));
        rvFlashCards.setNestedScrollingEnabled(false);

        // Adapter vocabulary — truyền TTS để phát âm
        cardAdapter = new FlashCardDetailAdapter(filteredCards, item -> showDotMenu(item));
        cardAdapter.setTts(tts);
        rvFlashCards.setAdapter(cardAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload progress mỗi khi quay lại (sau khi học xong)
        loadStudyProgress();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
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
        if (btnAddFlashcard != null) btnAddFlashcard.setOnClickListener(v -> {
            android.content.Intent i = new android.content.Intent(this, FlashCardListActivity.class);
            i.putExtra("flashcard_set_id",   flashCardSetId);
            i.putExtra("flashcard_set_name", getIntent().getStringExtra("flashcard_set_name"));
            startActivity(i);
        });

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

        int matchCount = 0;
        for (FlashCardResponse c : allCards) {
            if (c == null) {
                android.util.Log.w("FlashCardSetDetail", "Found null flashcard in allCards!");
                continue;
            }

            // Search match
            boolean matchQuery = q.isEmpty()
                    || (c.getTerm() != null && c.getTerm().toLowerCase().contains(q))
                    || (c.getDefinition() != null && c.getDefinition().toLowerCase().contains(q));

            // Filter: "all" luôn hiển thị tất cả từ có trong bộ
            // remembered/need_review/not_study: chưa có StudyRecord nên mặc định pass all
            boolean matchFilter = true; // mặc định match tất cả

            if (filter.equals("remembered")) {
                matchFilter = false; // TODO: check StudyRecord
            } else if (filter.equals("need_review")) {
                matchFilter = false; // TODO: check StudyRecord
            } else if (filter.equals("not_study")) {
                matchFilter = true; // chưa học = tất cả
            } else if (filter.equals("all")) {
                matchFilter = true; // show all
            }

            if (matchQuery && matchFilter) {
                filteredCards.add(c);
                matchCount++;
                android.util.Log.d("FlashCardSetDetail", "Added card " + matchCount + ": " + c.getTerm());
            }
        }

        if (cardAdapter != null) {
            cardAdapter.notifyDataSetChanged();
        }

        // Cập nhật số từ hiển thị
        if (tvFlashCardCount != null) {
            tvFlashCardCount.setText(String.format(Locale.getDefault(), "%d từ", allCards.size()));
        }

        // DEBUG: Log để kiểm tra
        android.util.Log.d("FlashCardSetDetail",
                "applyFilter: allCards=" + allCards.size() + ", filteredCards=" + filteredCards.size()
                        + ", filter=" + filter + ", query=" + q);
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

    // ── Toolbar 3-dot → PopupWindow icon ngang ───────────────
    private void showMoreMenu() {
        View popupView = LayoutInflater.from(this)
                .inflate(R.layout.popup_set_actions, null);

        PopupWindow popup = new PopupWindow(
                popupView,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        popup.setElevation(12f);
        popup.setOutsideTouchable(true);
        popup.setBackgroundDrawable(
                androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_popup_rounded));

        ImageButton btnSave      = popupView.findViewById(R.id.btnActSave);
        ImageButton btnDownload  = popupView.findViewById(R.id.btnActDownload);
        ImageButton btnShare     = popupView.findViewById(R.id.btnActShare);
        ImageButton btnDuplicate = popupView.findViewById(R.id.btnActDuplicate);
        ImageButton btnPlay      = popupView.findViewById(R.id.btnActPlay);
        ImageButton btnEdit      = popupView.findViewById(R.id.btnActEdit);
        ImageButton btnDelete    = popupView.findViewById(R.id.btnActDelete);

        // Owner: ẩn Bookmark, hiện Edit + Delete
        // Không phải owner: hiện Bookmark, ẩn Edit + Delete
        if (btnSave != null) {
            btnSave.setVisibility(isOwner ? View.GONE : View.VISIBLE);
            btnSave.setOnClickListener(x -> {
                popup.dismiss();
                Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
            });
        }
        if (btnDownload != null) btnDownload.setOnClickListener(x -> {
            popup.dismiss();
            Toast.makeText(this, "Download — coming soon", Toast.LENGTH_SHORT).show();
        });
        if (btnShare != null) btnShare.setOnClickListener(x -> {
            popup.dismiss();
            android.content.ClipboardManager cm =
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(
                    android.content.ClipData.newPlainText("link",
                            "estudy://set/" + flashCardSetId));
            Toast.makeText(this, "Link copied!", Toast.LENGTH_SHORT).show();
        });
        if (btnDuplicate != null) btnDuplicate.setOnClickListener(x -> {
            popup.dismiss();
            Toast.makeText(this, "Duplicate — coming soon", Toast.LENGTH_SHORT).show();
        });
        if (btnPlay != null) btnPlay.setOnClickListener(x -> {
            popup.dismiss();
            Toast.makeText(this, "Auto play — coming soon", Toast.LENGTH_SHORT).show();
        });
        if (btnEdit != null) {
            btnEdit.setVisibility(isOwner ? View.VISIBLE : View.GONE);
            btnEdit.setOnClickListener(x -> {
                popup.dismiss();
                Intent intent = new Intent(this, FlashCardSetEditActivity.class);
                intent.putExtra("flashcard_set_id", flashCardSetId);
                intent.putExtra("flashcard_set_name",
                        getIntent().getStringExtra("flashcard_set_name"));
                NavHelper.go(this, intent);
            });
        }
        if (btnDelete != null) {
            btnDelete.setVisibility(isOwner ? View.VISIBLE : View.GONE);
            btnDelete.setOnClickListener(x -> {
                popup.dismiss();
                confirmDeleteSet();
            });
        }

        // Hiện popup bên dưới nút btnMore
        View anchor = findViewById(R.id.btnMore);
        if (anchor != null) {
            popup.showAsDropDown(anchor, 0, 4);
        }
    }

    private void confirmDeleteSet() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Xóa bộ flashcard?")
                .setMessage("Bộ này sẽ bị xóa vĩnh viễn.")
                .setPositiveButton("Xóa", (d, w) ->
                        apiService.deleteFlashCardSet(flashCardSetId)
                                .enqueue(new Callback<ApiResponse<Void>>() {
                                    @Override public void onResponse(Call<ApiResponse<Void>> c,
                                                                     Response<ApiResponse<Void>> r) {
                                        if (r.isSuccessful()) {
                                            Toast.makeText(FlashCardSetDetailActivity.this,
                                                    "Đã xóa", Toast.LENGTH_SHORT).show();
                                            finish();
                                        }
                                    }
                                    @Override public void onFailure(Call<ApiResponse<Void>> c, Throwable t) {}
                                }))
                .setNegativeButton("Hủy", null).show();
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

                        // DEBUG: Log số item từ API
                        android.util.Log.d("FlashCardSetDetail", "Received " + allCards.size() + " flashcards from API");

                        applyFilter(currentFilter, "");

                        // Load progress thực từ API
                        loadStudyProgress();

                        // Privacy
                        isPublic = "PUBLIC".equals(data.getPrivacy());
                        // Kiểm tra owner để hiện/ẩn Edit & Delete
                        String ownerUsername = data.getOwnerUsername();
                        String currentUsername = tokenManager.getCurrentUsername();
                        isOwner = currentUsername != null
                                && currentUsername.equals(ownerUsername);


                    }

                    @Override
                    public void onFailure(Call<ApiResponse<FlashCardSetDetailResponse>> call,
                                          Throwable t) {
                        Toast.makeText(FlashCardSetDetailActivity.this,
                                "Load failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadStudyProgress() {
        apiService.getSetProgress()
                .enqueue(new Callback<ApiResponse<List<SetProgressResponse>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<SetProgressResponse>>> call,
                                           Response<ApiResponse<List<SetProgressResponse>>> res) {
                        if (!res.isSuccessful() || res.body() == null
                                || res.body().getResult() == null) {
                            // Fallback: tất cả chưa học
                            updateProgress(0, 0, allCards.size());
                            return;
                        }
                        boolean found = false;
                        for (SetProgressResponse sp : res.body().getResult()) {
                            if (flashCardSetId.equals(sp.getSetId())) {
                                updateProgress(
                                        sp.getRememberedCount(),
                                        sp.getNotYetCount(),
                                        sp.getNotStudiedCount()
                                );
                                found = true;
                                break;
                            }
                        }
                        if (!found) updateProgress(0, 0, allCards.size());
                    }
                    @Override
                    public void onFailure(Call<ApiResponse<List<SetProgressResponse>>> call,
                                          Throwable t) {
                        updateProgress(0, 0, allCards.size());
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



}