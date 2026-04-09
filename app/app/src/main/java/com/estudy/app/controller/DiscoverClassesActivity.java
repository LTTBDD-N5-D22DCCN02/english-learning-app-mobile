package com.estudy.app.controller;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.estudy.app.R;
import com.estudy.app.api.ApiClient;
import com.estudy.app.api.ApiService;
import com.estudy.app.model.response.ApiResponse;
import com.estudy.app.model.response.ClassResponse;
import com.estudy.app.utils.TokenManager;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DiscoverClassesActivity extends AppCompatActivity {

    private RecyclerView rvPublicClasses;
    private TextView tvEmpty;
    private EditText etSearch;
    private ApiService apiService;
    private TokenManager tokenManager;
    private List<ClassResponse> allClasses = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover_classes);

        tokenManager = new TokenManager(this);
        apiService = ApiClient.getInstance(tokenManager).create(ApiService.class);

        ImageButton btnBack = findViewById(R.id.btnBack);
        rvPublicClasses = findViewById(R.id.rvPublicClasses);
        tvEmpty = findViewById(R.id.tvEmpty);
        etSearch = findViewById(R.id.etSearch);

        btnBack.setOnClickListener(v -> finish());
        rvPublicClasses.setLayoutManager(new LinearLayoutManager(this));

        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                loadPublicClasses(s.toString().trim());
            }
            public void afterTextChanged(Editable s) {}
        });

        loadPublicClasses("");
    }

    private void loadPublicClasses(String keyword) {
        apiService.getPublicClasses(keyword).enqueue(new Callback<ApiResponse<List<ClassResponse>>>() {
            public void onResponse(Call<ApiResponse<List<ClassResponse>>> call,
                                   Response<ApiResponse<List<ClassResponse>>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getResult() != null) {
                    List<ClassResponse> list = response.body().getResult();
                    if (list.isEmpty()) {
                        rvPublicClasses.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        rvPublicClasses.setVisibility(View.VISIBLE);
                        tvEmpty.setVisibility(View.GONE);
                        PublicClassAdapter adapter = new PublicClassAdapter(list, cls -> joinPublicClass(cls));
                        rvPublicClasses.setAdapter(adapter);
                    }
                }
            }
            public void onFailure(Call<ApiResponse<List<ClassResponse>>> call, Throwable t) {
                Toast.makeText(DiscoverClassesActivity.this, "Load failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void joinPublicClass(ClassResponse cls) {
        apiService.joinPublicClass(cls.getId()).enqueue(new Callback<ApiResponse<Void>>() {
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> r) {
                if (r.isSuccessful()) {
                    Toast.makeText(DiscoverClassesActivity.this,
                            "Request sent. Waiting for approval.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(DiscoverClassesActivity.this,
                            "Error: " + r.code(), Toast.LENGTH_SHORT).show();
                }
            }
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(DiscoverClassesActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}