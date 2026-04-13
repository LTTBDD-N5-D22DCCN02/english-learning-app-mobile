package com.estudy.app.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
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
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView rvFlashCardSets;
    private TextView tvSeeAllSets, tvClassCount;
    private CardView cardClasses;
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

        rvFlashCardSets = findViewById(R.id.rvFlashCardSets);
        tvSeeAllSets = findViewById(R.id.tvSeeAllSets);
        tvClassCount = findViewById(R.id.tvClassCount);
        cardClasses = findViewById(R.id.cardClasses);

        rvFlashCardSets.setLayoutManager(new LinearLayoutManager(this));

        // ── Toolbar buttons ──────────────────────────────────────────
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
        btnProfile.setOnClickListener(v -> showLogoutDialog());

        // ── See all links ─────────────────────────────────────────────
        // See all links
        tvSeeAllSets.setOnClickListener(v ->
                startActivity(new Intent(this, FlashCardSetListActivity.class)));
        TextView tvSeeAllClasses = findViewById(R.id.tvSeeAllClasses);
        tvSeeAllClasses.setOnClickListener(v -> goToMyClasses());

        // ── Classes card (big banner) ─────────────────────────────────
        cardClasses.setOnClickListener(v -> goToMyClasses());

        // ── Quick access buttons ──────────────────────────────────────
        View btnQuickMyClasses = findViewById(R.id.btnQuickMyClasses);
        View btnQuickDiscover = findViewById(R.id.btnQuickDiscover);
        View btnQuickCreateClass = findViewById(R.id.btnQuickCreateClass);

        btnQuickMyClasses.setOnClickListener(v -> goToMyClasses());
        btnQuickDiscover.setOnClickListener(v ->
                startActivity(new Intent(this, DiscoverClassesActivity.class)));
        btnQuickCreateClass.setOnClickListener(v ->
                startActivity(new Intent(this, CreateClassActivity.class)));

        // ── Bottom navigation ─────────────────────────────────────────
        setupBottomNav();

        loadFlashCardSets();
        loadClassCount();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFlashCardSets();
        loadClassCount();
    }

    private void goToMyClasses() {
        startActivity(new Intent(this, MyClassesActivity.class));
    }

    private void setupBottomNav() {
        View btnNavHome    = findViewById(R.id.btnNavHome);
        View btnNavSets    = findViewById(R.id.btnNavSets);
        View btnNavAdd     = findViewById(R.id.btnNavAdd);
        View btnNavClasses = findViewById(R.id.btnNavClasses);
        View btnNavNotif   = findViewById(R.id.btnNavNotif);
        View btnNavStats   = findViewById(R.id.btnNavStats);

        // Highlight active tab
        highlightNavTab(btnNavHome);

        btnNavHome.setOnClickListener(v -> { /* đang ở Home */ });
        btnNavSets.setOnClickListener(v ->
                startActivity(new Intent(this, FlashCardSetListActivity.class)));
        btnNavAdd.setOnClickListener(v ->
                startActivity(new Intent(this, FlashCardSetCreateActivity.class)));
        btnNavClasses.setOnClickListener(v -> goToMyClasses());
        btnNavNotif.setOnClickListener(v ->
                Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show());
        btnNavStats.setOnClickListener(v ->
                Toast.makeText(this, "Stats coming soon", Toast.LENGTH_SHORT).show());
    }
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

    /** Làm sáng tab đang active bằng cách tăng alpha icon & text */
    private void highlightNavTab(View tabLayout) {
        if (tabLayout instanceof LinearLayout) {
            ((LinearLayout) tabLayout).setAlpha(1f);
            // Thêm indicator line trên cùng
            tabLayout.setBackground(getResources().getDrawable(R.drawable.bg_nav_active, null));
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
        apiService.logout(new LogoutRequest(token)).enqueue(new Callback<ApiResponse<Void>>() {
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                clearAndGoToLogin();
            }
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                clearAndGoToLogin();
            }
        });
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

    /** Load số lớp học để hiển thị trong card */
    private void loadClassCount() {
        apiService.getMyClasses().enqueue(new Callback<ApiResponse<List<ClassResponse>>>() {
            public void onResponse(Call<ApiResponse<List<ClassResponse>>> call,
                                   Response<ApiResponse<List<ClassResponse>>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getResult() != null) {
                    int count = response.body().getResult().size();
                    runOnUiThread(() -> {
                        if (count == 0) {
                            tvClassCount.setText("Tap to create or join a class");
                        } else {
                            tvClassCount.setText(count + " class" + (count > 1 ? "es" : "") + " joined");
                        }
                    });
                }
            }
            public void onFailure(Call<ApiResponse<List<ClassResponse>>> call, Throwable t) {}
        });
    }

    private void loadFlashCardSets() {
        apiService.getMyFlashCardSets().enqueue(new Callback<ApiResponse<List<FlashCardSetResponse>>>() {
            public void onResponse(Call<ApiResponse<List<FlashCardSetResponse>>> call,
                                   Response<ApiResponse<List<FlashCardSetResponse>>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getResult() != null) {
                    List<FlashCardSetResponse> list = response.body().getResult();
                    List<FlashCardSetResponse> preview = list.size() > 3
                            ? list.subList(0, 3) : list;

                    FlashCardSetAdapter adapter = new FlashCardSetAdapter(
                            preview,
                            item -> {
                                Intent intent = new Intent(HomeActivity.this,
                                        FlashCardSetDetailActivity.class);
                                intent.putExtra("flashcard_set_id", item.getId());
                                intent.putExtra("flashcard_set_name", item.getName());
                                startActivity(intent);
                            },
                            item -> {
                                Intent intent = new Intent(HomeActivity.this,
                                        FlashCardSetEditActivity.class);
                                intent.putExtra("flashcard_set_id", item.getId());
                                intent.putExtra("flashcard_set_name", item.getName());
                                intent.putExtra("flashcard_set_description", item.getDescription());
                                intent.putExtra("flashcard_set_privacy", item.getPrivacy());
                                startActivity(intent);
                            },
                            item -> {}
                    );
                    rvFlashCardSets.setAdapter(adapter);
                }
            }
            public void onFailure(Call<ApiResponse<List<FlashCardSetResponse>>> call, Throwable t) {
                Toast.makeText(HomeActivity.this,
                        "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
                tokenManager.clearToken();
                startActivity(new Intent(HomeActivity.this, LoginActivity.class));
                finish();
            }
        });
    }
}