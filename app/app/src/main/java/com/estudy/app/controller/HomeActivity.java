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
import com.estudy.app.model.response.FlashCardSetResponse;
import com.estudy.app.model.response.StudyTodayResponse;
import com.estudy.app.utils.BottomNavHelper;
import com.estudy.app.utils.TokenManager;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    // ── Views ───
    private RecyclerView rvFlashCardSets, rvClasses;
    private TextView tvSeeAllSets, tvSeeAllClasses, tvEmptySets, tvEmptyClasses;
    private TextView tvTotalTerms, tvDueCount, tvNewCount, tvDoneCount;
    private Button btnStudyAll;
    private View cardReviewToday;

    private ApiService apiService;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        BottomNavHelper.setup(this, R.id.btnNavHome);

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
    }

    // ──────────────────────────────────────────────────────────────
    // Bind views
    // ──────────────────────────────────────────────────────────────
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
        btnStudyAll      = findViewById(R.id.btnStudyAll);
        cardReviewToday  = findViewById(R.id.cardReviewToday);

        // Carousel ngang cho Flashcard Sets
        rvFlashCardSets.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Carousel ngang cho Classes
        rvClasses.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    // ──────────────────────────────────────────────────────────────
    // Setup navigation clicks
    // ──────────────────────────────────────────────────────────────
    private void setupNavigation() {
        // Toolbar icons
        ImageButton btnLibrary = findViewById(R.id.btnLibrary);
        ImageButton btnProfile = findViewById(R.id.btnProfile);
        btnLibrary.setOnClickListener(v ->
                startActivity(new Intent(this, FlashCardSetListActivity.class)));
        btnProfile.setOnClickListener(v -> showProfileMenu());

        // See all links
        tvSeeAllSets.setOnClickListener(v ->
                startActivity(new Intent(this, FlashCardSetListActivity.class)));
        tvSeeAllClasses.setOnClickListener(v ->
                Toast.makeText(this, "Classes — coming soon!", Toast.LENGTH_SHORT).show());
        // TODO: startActivity(new Intent(this, ClassListActivity.class));

        // Banner "Study right now!" + nút
        cardReviewToday.setOnClickListener(v ->
                startActivity(new Intent(this, StudyTodayActivity.class)));
        btnStudyAll.setOnClickListener(v ->
                startActivity(new Intent(this, StudyTodayActivity.class)));

    }

    // ──────────────────────────────────────────────────────────────
    // Load tất cả data cùng lúc
    // ──────────────────────────────────────────────────────────────
    private void loadData() {
        loadStudyStats();    // Due/New/Done numbers trên banner
        loadFlashCardSets(); // Carousel ngang flashcard sets
        // loadClasses();    // TODO: khi Yến làm xong Classes API
        showEmptyClasses();  // Tạm thời hiển thị empty
    }

    // ──────────────────────────────────────────────────────────────
    // Load study stats cho banner "Review today"
    // ──────────────────────────────────────────────────────────────
    private void loadStudyStats() {
        apiService.getStudyToday().enqueue(new Callback<ApiResponse<StudyTodayResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<StudyTodayResponse>> call,
                                   Response<ApiResponse<StudyTodayResponse>> response) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().getResult() != null) {
                    StudyTodayResponse data = response.body().getResult();

                    int due   = data.getTotalDue();
                    int newW  = data.getTotalNew();
                    int total = due + newW;

                    tvDueCount.setText(String.valueOf(due));
                    tvNewCount.setText(String.valueOf(newW));
                    tvDoneCount.setText("0");
                    tvTotalTerms.setText(total + " terms");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<StudyTodayResponse>> call, Throwable t) {
                // Silent fail — banner vẫn hiển thị với số 0
            }
        });
    }

    // ──────────────────────────────────────────────────────────────
    // Load flashcard sets → hiển thị carousel ngang
    // ──────────────────────────────────────────────────────────────
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
                                rvFlashCardSets.setVisibility(View.GONE);
                                tvEmptySets.setVisibility(View.VISIBLE);
                                return;
                            }

                            rvFlashCardSets.setVisibility(View.VISIBLE);
                            tvEmptySets.setVisibility(View.GONE);

                            // Adapter horizontal — dùng FlashCardSetAdapter hiện có
                            // nhưng inflate layout item_flashcard_set_horizontal
                            FlashCardSetHorizontalAdapter adapter =
                                    new FlashCardSetHorizontalAdapter(list, item -> {
                                        Intent intent = new Intent(HomeActivity.this,
                                                FlashCardSetOverviewActivity.class);
                                        intent.putExtra("flashcard_set_id",   item.getId());
                                        intent.putExtra("flashcard_set_name", item.getName());
                                        startActivity(intent);
                                    });
                            rvFlashCardSets.setAdapter(adapter);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<FlashCardSetResponse>>> call, Throwable t) {
                        Toast.makeText(HomeActivity.this,
                                "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
                        tokenManager.clearToken();
                        startActivity(new Intent(HomeActivity.this, LoginActivity.class));
                        finish();
                    }
                });
    }

    // ──────────
    // Classes:
    // ──────────
    private void showEmptyClasses() {
        rvClasses.setVisibility(View.GONE);
        tvEmptyClasses.setVisibility(View.VISIBLE);
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
    // Logout
    // ─────────
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