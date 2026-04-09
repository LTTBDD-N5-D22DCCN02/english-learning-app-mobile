package com.estudy.app.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.estudy.app.R;
import com.estudy.app.api.ApiClient;
import com.estudy.app.api.ApiService;
import com.estudy.app.model.request.LogoutRequest;
import com.estudy.app.model.response.ApiResponse;
import com.estudy.app.model.response.ClassResponse;
import com.estudy.app.model.response.FlashCardSetResponse;
import com.estudy.app.utils.TokenManager;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView rvFlashCardSets;
    private TextView tvSeeAllSets, tvClassCount;
    private CardView cardClasses;
    private ApiService apiService;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tokenManager = new TokenManager(this);
        apiService = ApiClient.getInstance(tokenManager).create(ApiService.class);

        rvFlashCardSets = findViewById(R.id.rvFlashCardSets);
        tvSeeAllSets = findViewById(R.id.tvSeeAllSets);
        tvClassCount = findViewById(R.id.tvClassCount);
        cardClasses = findViewById(R.id.cardClasses);

        rvFlashCardSets.setLayoutManager(new LinearLayoutManager(this));

        // ── Toolbar buttons ──────────────────────────────────────────
        ImageButton btnLibrary = findViewById(R.id.btnLibrary);
        ImageButton btnProfile = findViewById(R.id.btnProfile);
        btnLibrary.setOnClickListener(v ->
                startActivity(new Intent(this, FlashCardSetListActivity.class)));
        btnProfile.setOnClickListener(v -> showLogoutDialog());

        // ── See all links ─────────────────────────────────────────────
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

    /** Làm sáng tab đang active bằng cách tăng alpha icon & text */
    private void highlightNavTab(View tabLayout) {
        if (tabLayout instanceof LinearLayout) {
            ((LinearLayout) tabLayout).setAlpha(1f);
            // Thêm indicator line trên cùng
            tabLayout.setBackground(getResources().getDrawable(R.drawable.bg_nav_active, null));
        }
    }

    private void showLogoutDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> callLogoutApi())
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