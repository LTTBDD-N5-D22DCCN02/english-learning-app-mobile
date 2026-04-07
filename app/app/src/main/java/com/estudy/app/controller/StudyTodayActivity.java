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
import com.estudy.app.model.response.StudySetItem;
import com.estudy.app.model.response.StudyTodayResponse;
import com.estudy.app.utils.TokenManager;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudyTodayActivity extends AppCompatActivity {

    private TextView tvSubtitle, tvDueCount, tvNewCount, tvDoneCount;
    private TextView tvDueTotalLabel, tvNewTotalLabel;
    private Button btnStudyAll;
    private RecyclerView rvDueSets, rvNewSets;
    private LinearLayout layoutDueSection, layoutNewSection, layoutEmpty;
    private ProgressBar progressBar;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_today);

        TokenManager tokenManager = new TokenManager(this);
        apiService = ApiClient.getInstance(tokenManager).create(ApiService.class);

        setupToolbar();
        setupBottomNav();
        bindViews();
        loadStudyToday();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStudyToday();
    }

    // ── Dùng toolbar_common.xml ────────────────────────────────────
    private void setupToolbar() {
        // toolbar_common có id: btnBack, tvToolbarTitle, btnSettings
        View toolbar = findViewById(R.id.toolbar);
        if (toolbar == null) return;

        // Set title
        TextView tvTitle = toolbar.findViewById(R.id.tvToolbarTitle);
        if (tvTitle != null) tvTitle.setText("Study today");

        // Back button
        ImageButton btnBack = toolbar.findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Settings button
        ImageButton btnSettings = toolbar.findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v ->
                    Toast.makeText(this, "Settings — coming soon!", Toast.LENGTH_SHORT).show());
        }
    }

    // ── Bottom nav — wire tất cả 5 button ──────────────────────────
    private void setupBottomNav() {
        View btnHome  = findViewById(R.id.btnNavHome);
        View btnSets  = findViewById(R.id.btnNavSets);
        View btnAdd   = findViewById(R.id.btnNavAdd);
        View btnNotif = findViewById(R.id.btnNavNotif);
        View btnStats = findViewById(R.id.btnNavStats);

        if (btnHome != null) btnHome.setOnClickListener(v -> {
            Intent i = new Intent(this, HomeActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        });
        if (btnSets  != null) btnSets.setOnClickListener(v ->
                startActivity(new Intent(this, FlashCardSetListActivity.class)));
        if (btnAdd   != null) btnAdd.setOnClickListener(v ->
                startActivity(new Intent(this, FlashCardSetCreateActivity.class)));
        if (btnNotif != null) btnNotif.setOnClickListener(v ->
                Toast.makeText(this, "Notifications — coming soon!", Toast.LENGTH_SHORT).show());
        if (btnStats != null) btnStats.setOnClickListener(v -> { /* đang ở đây */ });
    }

    // ── Bind content views ─────────────────────────────────────────
    private void bindViews() {
        tvSubtitle       = findViewById(R.id.tvSubtitle);
        tvDueCount       = findViewById(R.id.tvDueCount);
        tvNewCount       = findViewById(R.id.tvNewCount);
        tvDoneCount      = findViewById(R.id.tvDoneCount);
        tvDueTotalLabel  = findViewById(R.id.tvDueTotalLabel);
        tvNewTotalLabel  = findViewById(R.id.tvNewTotalLabel);
        btnStudyAll      = findViewById(R.id.btnStudyAll);
        rvDueSets        = findViewById(R.id.rvDueSets);
        rvNewSets        = findViewById(R.id.rvNewSets);
        layoutDueSection = findViewById(R.id.layoutDueSection);
        layoutNewSection = findViewById(R.id.layoutNewSection);
        layoutEmpty      = findViewById(R.id.layoutEmpty);
        progressBar      = findViewById(R.id.progressBar);

        rvDueSets.setLayoutManager(new LinearLayoutManager(this));
        rvNewSets.setLayoutManager(new LinearLayoutManager(this));
        rvDueSets.setNestedScrollingEnabled(false);
        rvNewSets.setNestedScrollingEnabled(false);

        // Icon gamepad trên section Due → mở BottomSheet cho tất cả due sets
        View btnDueMode = findViewById(R.id.btnDueMode);
        if (btnDueMode != null) {
            btnDueMode.setOnClickListener(v ->
                    showModeBottomSheet("", "Due today - All sets"));
        }
    }

    // ── Load API ───────────────────────────────────────────────────
    private void loadStudyToday() {
        showLoading(true);

        apiService.getStudyToday().enqueue(new Callback<ApiResponse<StudyTodayResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<StudyTodayResponse>> call,
                                   Response<ApiResponse<StudyTodayResponse>> response) {
                showLoading(false);
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().getResult() != null) {
                    bindData(response.body().getResult());
                } else {
                    Toast.makeText(StudyTodayActivity.this,
                            "Không thể tải dữ liệu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<StudyTodayResponse>> call, Throwable t) {
                showLoading(false);
                Toast.makeText(StudyTodayActivity.this,
                        "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindData(StudyTodayResponse data) {
        int totalDue = data.getTotalDue();
        int totalNew = data.getTotalNew();
        int total    = totalDue + totalNew;

        tvDueCount.setText(String.valueOf(totalDue));
        tvNewCount.setText(String.valueOf(totalNew));
        tvDoneCount.setText("0");
        tvSubtitle.setText(total + " words · " + countSets(data) + " sets");

        // Nút Study all
        if (btnStudyAll != null) {
            btnStudyAll.setText("Study all (" + total + ")");
            btnStudyAll.setVisibility(total > 0 ? View.VISIBLE : View.GONE);
            btnStudyAll.setOnClickListener(v -> showModeBottomSheet("", "All sets"));
        }

        // Section Due
        List<StudySetItem> dueSets = data.getDueSets();
        if (dueSets != null && !dueSets.isEmpty()) {
            layoutDueSection.setVisibility(View.VISIBLE);
            tvDueTotalLabel.setText(totalDue + " words");
            StudySetAdapter dueAdapter = new StudySetAdapter(dueSets, true,
                    item -> showModeBottomSheet(item.getSetId(), item.getSetName()));
            dueAdapter.setOnArrowClickListener(item -> openSetWords(item, true));
            rvDueSets.setAdapter(dueAdapter);
        } else {
            layoutDueSection.setVisibility(View.GONE);
        }

        // Section New
        List<StudySetItem> newSets = data.getNewSets();
        if (newSets != null && !newSets.isEmpty()) {
            layoutNewSection.setVisibility(View.VISIBLE);
            tvNewTotalLabel.setText(totalNew + " words");
            StudySetAdapter newAdapter = new StudySetAdapter(newSets, false,
                    item -> showModeBottomSheet(item.getSetId(), item.getSetName()));
            newAdapter.setOnArrowClickListener(item -> openSetWords(item, false));
            rvNewSets.setAdapter(newAdapter);
        } else {
            layoutNewSection.setVisibility(View.GONE);
        }

        // Empty
        if (total == 0) {
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    // ── Mở word list của 1 bộ ─────────────────────────────────────
    private void openSetWords(StudySetItem item, boolean isDue) {
        Intent i = new Intent(this, StudySetWordsActivity.class);
        i.putExtra(StudySetWordsActivity.EXTRA_SET_ID, item.getSetId());
        i.putExtra(StudySetWordsActivity.EXTRA_SET_NAME, item.getSetName());
        i.putExtra(StudySetWordsActivity.EXTRA_IS_DUE, isDue);
        startActivity(i);
    }

    // ── Mở bottom sheet chọn chế độ học ───────────────────────────
    private void showModeBottomSheet(String setId, String setName) {
        StudyModeBottomSheet sheet =
                StudyModeBottomSheet.newInstance(setId, setName);
        sheet.show(getSupportFragmentManager(), "StudyMode");
    }

    private int countSets(StudyTodayResponse data) {
        Set<String> ids = new HashSet<>();
        if (data.getDueSets() != null)
            for (StudySetItem s : data.getDueSets()) ids.add(s.getSetId());
        if (data.getNewSets() != null)
            for (StudySetItem s : data.getNewSets()) ids.add(s.getSetId());
        return ids.size();
    }

    private void showLoading(boolean loading) {
        if (progressBar != null)
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}