package com.estudy.app.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.estudy.app.R;
import com.estudy.app.api.ApiClient;
import com.estudy.app.api.ApiService;
import com.estudy.app.model.response.ApiResponse;
import com.estudy.app.model.response.NotificationMetadata;
import com.estudy.app.model.response.NotificationResponse;
import com.estudy.app.utils.TokenManager;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.estudy.app.utils.BottomNavHelper;

public class NotificationActivity extends AppCompatActivity {

    // Filter constants
    private static final String FILTER_ALL           = "all";
    private static final String FILTER_CLASSES       = "classes";
    private static final String FILTER_SYSTEMS       = "systems";
    private static final String FILTER_FLASHCARD_SETS= "flashcard_sets";

    // Views
    private RecyclerView rvNotifications;
    private ProgressBar  progressBar;
    private TextView     tvEmpty;
    private View         layoutError;

    // Data
    private ApiService   apiService;
    private TokenManager tokenManager;
    private List<NotificationResponse> allNotifications = new ArrayList<>();
    private String currentFilter = FILTER_ALL;

    // ─────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        tokenManager = new TokenManager(this);
        apiService   = ApiClient.getInstance(tokenManager).create(ApiService.class);

        bindViews();
        setupNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications();
        BottomNavHelper.loadBadge(this, apiService);
    }

    // ─────────────────────────────────────────────────────────────
    // Bind views
    // ─────────────────────────────────────────────────────────────
    private void bindViews() {
        rvNotifications = findViewById(R.id.rvNotifications);
        progressBar     = findViewById(R.id.progressBar);
        tvEmpty         = findViewById(R.id.tvEmpty);
        layoutError     = findViewById(R.id.layoutError);

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));

        Button btnRetry = findViewById(R.id.btnRetry);
        if (btnRetry != null) btnRetry.setOnClickListener(v -> loadNotifications());
    }

    // ─────────────────────────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────────────────────────
    private void setupNavigation() {
        ImageButton btnBack   = findViewById(R.id.btnBack);
        ImageButton btnFilter = findViewById(R.id.btnFilter);

        btnBack.setOnClickListener(v -> finish());
        btnFilter.setOnClickListener(v -> showFilterPopup(v));

        // Bottom nav — highlight Notifications tab
        View btnNavHome  = findViewById(R.id.btnNavHome);
        View btnNavSets  = findViewById(R.id.btnNavSets);
        View btnNavAdd   = findViewById(R.id.btnNavAdd);
        View btnNavNotif = findViewById(R.id.btnNavNotif);
        View btnNavStats = findViewById(R.id.btnNavStats);

        if (btnNavHome  != null) btnNavHome.setOnClickListener(v  -> { finish(); });
        if (btnNavSets  != null) btnNavSets.setOnClickListener(v  ->
                startActivity(new Intent(this, FlashCardSetListActivity.class)));
        if (btnNavAdd   != null) btnNavAdd.setOnClickListener(v   ->
                startActivity(new Intent(this, FlashCardSetCreateActivity.class)));
        if (btnNavStats != null) btnNavStats.setOnClickListener(v ->
                startActivity(new Intent(this, StatisticsActivity.class)));

        // Highlight active tab
        if (btnNavNotif != null) btnNavNotif.setAlpha(1.0f);
        if (btnNavHome  != null) btnNavHome.setAlpha(0.55f);
        if (btnNavSets  != null) btnNavSets.setAlpha(0.55f);
        if (btnNavAdd   != null) btnNavAdd.setAlpha(0.55f);
        if (btnNavStats != null) btnNavStats.setAlpha(0.55f);
    }

    // ─────────────────────────────────────────────────────────────
    // Load data
    // ─────────────────────────────────────────────────────────────
    private void loadNotifications() {
        showLoading();
        apiService.getNotifications().enqueue(new Callback<ApiResponse<List<NotificationResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<NotificationResponse>>> call,
                                   Response<ApiResponse<List<NotificationResponse>>> response) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().getResult() != null) {
                    allNotifications = response.body().getResult();
                    applyFilter(currentFilter);
                } else {
                    showError();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<NotificationResponse>>> call, Throwable t) {
                showError();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    // Filter (client-side)
    // ─────────────────────────────────────────────────────────────
    private void applyFilter(String filter) {
        currentFilter = filter;
        List<NotificationResponse> filtered;

        switch (filter) {
            case FILTER_CLASSES:
                filtered = allNotifications.stream()
                        .filter(n -> "join_request".equals(n.getType()))
                        .collect(Collectors.toList());
                break;
            case FILTER_SYSTEMS:
                filtered = allNotifications.stream()
                        .filter(n -> "vocab_reminder".equals(n.getType()))
                        .collect(Collectors.toList());
                break;
            case FILTER_FLASHCARD_SETS:
                filtered = allNotifications.stream()
                        .filter(n -> "new_set".equals(n.getType()) || "new_comment".equals(n.getType()))
                        .collect(Collectors.toList());
                break;
            default:
                filtered = new ArrayList<>(allNotifications);
                break;
        }

        showList(filtered);
    }

    private void showList(List<NotificationResponse> list) {
        progressBar.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);

        if (list.isEmpty()) {
            rvNotifications.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        tvEmpty.setVisibility(View.GONE);
        rvNotifications.setVisibility(View.VISIBLE);

        NotificationAdapter adapter = new NotificationAdapter(list, this::onNotificationClick);
        rvNotifications.setAdapter(adapter);
    }

    // ─────────────────────────────────────────────────────────────
    // Item click → mark read → navigate
    // ─────────────────────────────────────────────────────────────
    private void onNotificationClick(NotificationResponse notif) {
        if (!notif.isRead()) {
            markAsRead(notif);
        } else {
            navigate(notif);
        }
    }

    private void markAsRead(NotificationResponse notif) {
        apiService.markNotificationRead(notif.getId())
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call,
                                           Response<ApiResponse<Void>> response) {
                        notif.setRead(true);
                        applyFilter(currentFilter); // cập nhật dot ngay lập tức
                        BottomNavHelper.loadBadge(NotificationActivity.this, apiService);
                        navigate(notif);
                    }
                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        navigate(notif);
                    }
                });
    }

    private void navigate(NotificationResponse notif) {
        String type = notif.getType();
        NotificationMetadata meta = notif.getMetadata();

        if (type == null) return;

        switch (type) {
            case "vocab_reminder":
                startActivity(new Intent(this, StudyTodayActivity.class));
                break;

            case "join_request":
                if (meta == null) { showResourceNotFound(); return; }
                if ("approved".equals(meta.getStatus())) {
                    if (meta.getClassId() == null) { showResourceNotFound(); return; }
                    Intent i = new Intent(this, ClassDetailActivity.class);
                    i.putExtra("classId", meta.getClassId());
                    startActivity(i);
                } else if ("pending".equals(meta.getStatus())) {
                    if (meta.getClassId() == null) { showResourceNotFound(); return; }
                    Intent i = new Intent(this, ApproveRequestsActivity.class);
                    i.putExtra("classId", meta.getClassId());
                    startActivity(i);
                }
                break;

            case "new_set":
            case "new_comment":
                if (meta == null || meta.getSetId() == null) { showResourceNotFound(); return; }
                Intent i = new Intent(this, FlashCardSetDetailActivity.class);
                i.putExtra("flashcard_set_id",   meta.getSetId());
                i.putExtra("flashcard_set_name", "");
                startActivity(i);
                break;

            default:
                break;
        }
    }

    private void showResourceNotFound() {
        Toast.makeText(this, "Nội dung không còn tồn tại", Toast.LENGTH_SHORT).show();
    }

    // ─────────────────────────────────────────────────────────────
    // Filter popup
    // ─────────────────────────────────────────────────────────────
    private void showFilterPopup(View anchor) {
        View popupView = LayoutInflater.from(this)
                .inflate(R.layout.popup_filter_notification, null);

        PopupWindow popup = new PopupWindow(
                popupView,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                true);
        popup.setElevation(12f);
        popup.setOutsideTouchable(true);
        popup.setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        // Highlight selected option
        updateFilterHighlight(popupView);

        popupView.findViewById(R.id.btnClose).setOnClickListener(v -> popup.dismiss());

        popupView.findViewById(R.id.btnFilterAll).setOnClickListener(v -> {
            applyFilter(FILTER_ALL);
            popup.dismiss();
        });
        popupView.findViewById(R.id.btnFilterClasses).setOnClickListener(v -> {
            applyFilter(FILTER_CLASSES);
            popup.dismiss();
        });
        popupView.findViewById(R.id.btnFilterSystems).setOnClickListener(v -> {
            applyFilter(FILTER_SYSTEMS);
            popup.dismiss();
        });
        popupView.findViewById(R.id.btnFilterFlashcardSets).setOnClickListener(v -> {
            applyFilter(FILTER_FLASHCARD_SETS);
            popup.dismiss();
        });

        popup.showAsDropDown(anchor, 0, 8);
    }

    private void updateFilterHighlight(View popupView) {
        int colorSelected = getResources().getColor(R.color.input_background, null);
        int colorNormal   = android.graphics.Color.TRANSPARENT;

        popupView.findViewById(R.id.btnFilterAll)
                .setBackgroundColor(FILTER_ALL.equals(currentFilter)            ? colorSelected : colorNormal);
        popupView.findViewById(R.id.btnFilterClasses)
                .setBackgroundColor(FILTER_CLASSES.equals(currentFilter)        ? colorSelected : colorNormal);
        popupView.findViewById(R.id.btnFilterSystems)
                .setBackgroundColor(FILTER_SYSTEMS.equals(currentFilter)        ? colorSelected : colorNormal);
        popupView.findViewById(R.id.btnFilterFlashcardSets)
                .setBackgroundColor(FILTER_FLASHCARD_SETS.equals(currentFilter) ? colorSelected : colorNormal);
    }

    // ─────────────────────────────────────────────────────────────
    // UI state helpers
    // ─────────────────────────────────────────────────────────────
    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        rvNotifications.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
    }

    private void showError() {
        progressBar.setVisibility(View.GONE);
        rvNotifications.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.VISIBLE);
    }
}
