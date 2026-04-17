package com.estudy.app.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import android.widget.PopupWindow;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.estudy.app.R;
import com.estudy.app.api.ApiClient;
import com.estudy.app.api.ApiService;
import com.estudy.app.model.request.LogoutRequest;
import com.estudy.app.model.response.ApiResponse;
import com.estudy.app.model.response.ClassResponse;
import com.estudy.app.model.response.FlashCardSetResponse;
import com.estudy.app.model.response.StudyTodayResponse;
import com.estudy.app.utils.BottomNavHelper;
import com.estudy.app.utils.TokenManager;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────
    private RecyclerView rvFlashCardSets, rvClasses;
    private TextView tvSeeAllSets, tvSeeAllClasses;
    private TextView tvEmptySets, tvEmptyClasses;
    private TextView tvTotalTerms, tvDueCount, tvNewCount, tvDoneCount;
    private TextView tvClassCount;
    private Button   btnStudyAll;
    private View     cardReviewToday, cardClasses;

    // ── Data ──────────────────────────────────────────────────────
    private ApiService   apiService;
    private TokenManager tokenManager;

    // ─────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tokenManager = new TokenManager(this);
        apiService   = ApiClient.getInstance(tokenManager).create(ApiService.class);

        bindViews();
        setupNavigation();
        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
        BottomNavHelper.loadBadge(this, apiService);
    }

    // ─────────────────────────────────────────────────────────────
    // Bind views
    // ─────────────────────────────────────────────────────────────
    private void bindViews() {
        rvFlashCardSets  = findViewById(R.id.rvFlashCardSets);
        rvClasses        = findViewById(R.id.rvClasses);
        tvSeeAllSets     = findViewById(R.id.tvSeeAllSets);
        tvSeeAllClasses  = findViewById(R.id.tvSeeAllClasses);
        tvEmptySets      = findViewById(R.id.tvEmptySets);
        tvEmptyClasses   = findViewById(R.id.tvEmptyClasses);
        tvTotalTerms     = findViewById(R.id.tvTotalTerms);
        tvDueCount       = findViewById(R.id.tvDueCount);
        tvNewCount       = findViewById(R.id.tvNewCount);
        tvDoneCount      = findViewById(R.id.tvDoneCount);
        tvClassCount     = findViewById(R.id.tvClassCount);
        btnStudyAll      = findViewById(R.id.btnStudyAll);
        cardReviewToday  = findViewById(R.id.cardReviewToday);
        cardClasses      = findViewById(R.id.cardClasses);

        // Carousel ngang - Flashcard Sets
        rvFlashCardSets.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Carousel ngang - Classes
        if (rvClasses != null) {
            rvClasses.setLayoutManager(
                    new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────────────────────────
    private void setupNavigation() {
        // Toolbar
        ImageButton btnLibrary = findViewById(R.id.btnLibrary);
        ImageButton btnProfile = findViewById(R.id.btnProfile);
        if (btnLibrary != null) btnLibrary.setOnClickListener(v ->
                startActivity(new Intent(this, FlashCardSetListActivity.class)));
        btnProfile.setOnClickListener(v -> showProfileMenu());

        // See all
        if (tvSeeAllSets != null) tvSeeAllSets.setOnClickListener(v ->
                startActivity(new Intent(this, FlashCardSetListActivity.class)));
        if (tvSeeAllClasses != null) tvSeeAllClasses.setOnClickListener(v -> goToMyClasses());

        // Banner + study button
        if (cardReviewToday != null) cardReviewToday.setOnClickListener(v ->
                startActivity(new Intent(this, StudyTodayActivity.class)));
        if (btnStudyAll != null) btnStudyAll.setOnClickListener(v ->
                startActivity(new Intent(this, StudyTodayActivity.class)));

        // Classes card
        if (cardClasses != null) cardClasses.setOnClickListener(v -> goToMyClasses());

        // Quick access buttons
        View btnQuickMyClasses  = findViewById(R.id.btnQuickMyClasses);
        View btnQuickDiscover   = findViewById(R.id.btnQuickDiscover);
        View btnQuickCreateClass= findViewById(R.id.btnQuickCreateClass);
        if (btnQuickMyClasses   != null) btnQuickMyClasses.setOnClickListener(v -> goToMyClasses());
        if (btnQuickDiscover    != null) btnQuickDiscover.setOnClickListener(v ->
                startActivity(new Intent(this, DiscoverClassesActivity.class)));
        if (btnQuickCreateClass != null) btnQuickCreateClass.setOnClickListener(v ->
                startActivity(new Intent(this, CreateClassActivity.class)));

        // Bottom nav
        setupBottomNav();
    }

    private void setupBottomNav() {
        View btnNavHome    = findViewById(R.id.btnNavHome);
        View btnNavSets    = findViewById(R.id.btnNavSets);
        View btnNavAdd     = findViewById(R.id.btnNavAdd);
        View btnNavClasses = findViewById(R.id.btnNavClasses);
        View btnNavNotif   = findViewById(R.id.btnNavNotif);
        View btnNavStats   = findViewById(R.id.btnNavStats);

        if (btnNavHome    != null) btnNavHome.setOnClickListener(v -> { /* already here */ });
        if (btnNavSets    != null) btnNavSets.setOnClickListener(v ->
                startActivity(new Intent(this, FlashCardSetListActivity.class)));
        if (btnNavAdd     != null) btnNavAdd.setOnClickListener(v ->
                startActivity(new Intent(this, FlashCardSetCreateActivity.class)));
        if (btnNavClasses != null) btnNavClasses.setOnClickListener(v -> goToMyClasses());
        if (btnNavNotif   != null) btnNavNotif.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationActivity.class)));
        if (btnNavStats   != null) btnNavStats.setOnClickListener(v ->
                startActivity(new Intent(this, StatisticsActivity.class)));
    }

    private void goToMyClasses() {
        startActivity(new Intent(this, MyClassesActivity.class));
    }

    // ─────────────────────────────────────────────────────────────
    // Load data
    // ─────────────────────────────────────────────────────────────
    private void loadData() {
        loadStudyStats();
        loadFlashCardSets();
        loadClassCount();
        showEmptyClasses(); // Tạm thời cho đến khi Classes API hoàn thiện
    }

    private void loadStudyStats() {
        apiService.getStudyToday().enqueue(new Callback<ApiResponse<StudyTodayResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<StudyTodayResponse>> call,
                                   Response<ApiResponse<StudyTodayResponse>> response) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().getResult() != null) {
                    StudyTodayResponse data = response.body().getResult();
                    int due  = data.getTotalDue();
                    int newW = data.getTotalNew();
                    if (tvDueCount   != null) tvDueCount.setText(String.valueOf(due));
                    if (tvNewCount   != null) tvNewCount.setText(String.valueOf(newW));
                    if (tvDoneCount  != null) tvDoneCount.setText("0");
                    if (tvTotalTerms != null) tvTotalTerms.setText((due + newW) + " terms");
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<StudyTodayResponse>> call, Throwable t) {
                // Silent fail — banner hiện số 0
            }
        });
    }

    private void loadFlashCardSets() {
        apiService.getMyFlashCardSets().enqueue(
                new Callback<ApiResponse<List<FlashCardSetResponse>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<FlashCardSetResponse>>> call,
                                           Response<ApiResponse<List<FlashCardSetResponse>>> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().getResult() != null) {

                            List<FlashCardSetResponse> list = response.body().getResult();

                            if (list.isEmpty()) {
                                if (rvFlashCardSets != null) rvFlashCardSets.setVisibility(View.GONE);
                                if (tvEmptySets     != null) tvEmptySets.setVisibility(View.VISIBLE);
                                return;
                            }

                            if (rvFlashCardSets != null) rvFlashCardSets.setVisibility(View.VISIBLE);
                            if (tvEmptySets     != null) tvEmptySets.setVisibility(View.GONE);

                            FlashCardSetHorizontalAdapter adapter =
                                    new FlashCardSetHorizontalAdapter(list, item -> {
                                        Intent intent = new Intent(HomeActivity.this,
                                                FlashCardSetOverviewActivity.class);
                                        intent.putExtra("flashcard_set_id",   item.getId());
                                        intent.putExtra("flashcard_set_name", item.getName());
                                        startActivity(intent);
                                    });
                            if (rvFlashCardSets != null) rvFlashCardSets.setAdapter(adapter);
                        }
                    }
                    @Override
                    public void onFailure(Call<ApiResponse<List<FlashCardSetResponse>>> call,
                                          Throwable t) {
                        Toast.makeText(HomeActivity.this,
                                "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
                        tokenManager.clearToken();
                        startActivity(new Intent(HomeActivity.this, LoginActivity.class));
                        finish();
                    }
                });
    }

    private void loadClassCount() {
        apiService.getMyClasses().enqueue(new Callback<ApiResponse<List<ClassResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<ClassResponse>>> call,
                                   Response<ApiResponse<List<ClassResponse>>> response) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().getResult() != null) {
                    int count = response.body().getResult().size();
                    runOnUiThread(() -> {
                        if (tvClassCount != null) {
                            tvClassCount.setText(count == 0
                                    ? "Tap to create or join a class"
                                    : count + " class" + (count > 1 ? "es" : "") + " joined");
                        }
                    });
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<ClassResponse>>> call, Throwable t) {}
        });
    }

    // ─────────────────────────────────────────────────
    // Profile menu: View Profile / Logout
    // ─────────────────────────────────────────────────
    private void showProfileMenu() {
        View popupView = LayoutInflater.from(this)
                .inflate(R.layout.popup_profile_actions, null);

        PopupWindow popup = new PopupWindow(
                popupView,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        popup.setElevation(12f);
        popup.setOutsideTouchable(true);
        popup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                android.graphics.Color.TRANSPARENT));

        popupView.findViewById(R.id.btnViewProfile).setOnClickListener(v -> {
            popup.dismiss();
            startActivity(new Intent(this, ProfileActivity.class));
        });

        popupView.findViewById(R.id.btnLogout).setOnClickListener(v -> {
            popup.dismiss();
            showLogoutDialog();
        });

        // Anchor popup bên dưới icon btnProfile, căn phải
        View anchor = findViewById(R.id.btnProfile);
        popup.showAsDropDown(anchor, 0, 8);
    }

    // ─────────
    private void showEmptyClasses() {
        if (rvClasses      != null) rvClasses.setVisibility(View.GONE);
        if (tvEmptyClasses != null) tvEmptyClasses.setVisibility(View.VISIBLE);
    }

    // ─────────────────────────────────────────────────────────────
    // Logout
    // ─────────────────────────────────────────────────────────────
    private void showLogoutDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (d, w) -> callLogoutApi())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void callLogoutApi() {
        String token = tokenManager.getToken();
        apiService.logout(new LogoutRequest(token))
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call,
                                           Response<ApiResponse<Void>> response) {
                        clearAndGoToLogin();
                    }
                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        clearAndGoToLogin();
                    }
                });
    }

    private void clearAndGoToLogin() {
        tokenManager.clearToken();
        ApiClient.reset();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}