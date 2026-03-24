package com.estudy.app.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.estudy.app.R;
import com.estudy.app.api.ApiClient;
import com.estudy.app.api.ApiService;
import com.estudy.app.model.request.LogoutRequest;
import com.estudy.app.model.response.ApiResponse;
import com.estudy.app.model.response.FlashCardSetResponse;
import com.estudy.app.utils.TokenManager;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView rvFlashCardSets;
    private TextView tvSeeAllSets;
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
        rvFlashCardSets.setLayoutManager(
                new androidx.recyclerview.widget.LinearLayoutManager(this));

        // Toolbar icons
        ImageButton btnLibrary = findViewById(R.id.btnLibrary);
        ImageButton btnProfile = findViewById(R.id.btnProfile);

        btnLibrary.setOnClickListener(v ->
                startActivity(new Intent(this, FlashCardSetListActivity.class)));

        // Nhấn icon profile → hiện dialog logout
        btnProfile.setOnClickListener(v -> showLogoutDialog());

        // See all
        tvSeeAllSets.setOnClickListener(v ->
                startActivity(new Intent(this, FlashCardSetListActivity.class)));

        // Bottom nav
        View btnNavHome = findViewById(R.id.btnNavHome);
        View btnNavSets = findViewById(R.id.btnNavSets);
        View btnNavAdd = findViewById(R.id.btnNavAdd);

        btnNavHome.setOnClickListener(v -> { /* đang ở Home */ });
        btnNavSets.setOnClickListener(v ->
                startActivity(new Intent(this, FlashCardSetListActivity.class)));
        btnNavAdd.setOnClickListener(v ->
                startActivity(new Intent(this, FlashCardSetCreateActivity.class)));

        loadFlashCardSets();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFlashCardSets();
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

        apiService.logout(new LogoutRequest(token))
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call,
                                           Response<ApiResponse<Void>> response) {
                        clearAndGoToLogin();
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        // Mất mạng vẫn logout local
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

    private void loadFlashCardSets() {
        apiService.getMyFlashCardSets()
                .enqueue(new Callback<ApiResponse<List<FlashCardSetResponse>>>() {
                    @Override
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

                    @Override
                    public void onFailure(Call<ApiResponse<List<FlashCardSetResponse>>> call,
                                          Throwable t) {
                        Toast.makeText(HomeActivity.this,
                                "Session expired. Please login again.",
                                Toast.LENGTH_SHORT).show();
                        tokenManager.clearToken();
                        startActivity(new Intent(HomeActivity.this, LoginActivity.class));
                        finish();
                    }
                });
    }
}