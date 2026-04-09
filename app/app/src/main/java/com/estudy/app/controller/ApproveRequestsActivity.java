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
import com.estudy.app.model.response.ClassMemberResponse;
import com.estudy.app.utils.TokenManager;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ApproveRequestsActivity extends AppCompatActivity {

    private RecyclerView rvRequests;
    private TextView tvTotalRequests;
    private EditText etSearch;
    private ApiService apiService;
    private TokenManager tokenManager;
    private String classId;
    private List<ClassMemberResponse> allRequests = new ArrayList<>();
    private ApproveRequestAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_approve_requests);

        classId = getIntent().getStringExtra("classId");
        tokenManager = new TokenManager(this);
        apiService = ApiClient.getInstance(tokenManager).create(ApiService.class);

        View toolbar = findViewById(R.id.toolbar);
        ImageButton btnBack = toolbar.findViewById(R.id.btnBack);
        TextView tvTitle = toolbar.findViewById(R.id.tvToolbarTitle);
        tvTitle.setText("Approve Join Request");
        btnBack.setOnClickListener(v -> finish());

        rvRequests = findViewById(R.id.rvRequests);
        tvTotalRequests = findViewById(R.id.tvTotalRequests);
        etSearch = findViewById(R.id.etSearch);
        rvRequests.setLayoutManager(new LinearLayoutManager(this));

        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { filterList(s.toString().trim()); }
            public void afterTextChanged(Editable s) {}
        });

        loadRequests();
    }

    @Override
    protected void onResume() { super.onResume(); loadRequests(); }

    private void loadRequests() {
        apiService.getPendingRequests(classId).enqueue(new Callback<ApiResponse<List<ClassMemberResponse>>>() {
            public void onResponse(Call<ApiResponse<List<ClassMemberResponse>>> call,
                                   Response<ApiResponse<List<ClassMemberResponse>>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getResult() != null) {
                    allRequests = response.body().getResult();
                    updateUI(allRequests);
                }
            }
            public void onFailure(Call<ApiResponse<List<ClassMemberResponse>>> call, Throwable t) {
                Toast.makeText(ApproveRequestsActivity.this, "Load failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterList(String keyword) {
        if (keyword.isEmpty()) { updateUI(allRequests); return; }
        List<ClassMemberResponse> filtered = allRequests.stream()
                .filter(m -> {
                    String name = m.getFullName() != null ? m.getFullName() : m.getUsername();
                    return name != null && name.toLowerCase().contains(keyword.toLowerCase());
                })
                .collect(Collectors.toList());
        updateUI(filtered);
    }

    private void updateUI(List<ClassMemberResponse> list) {
        tvTotalRequests.setText("Total requests: " + list.size());
        adapter = new ApproveRequestAdapter(list,
                member -> approve(member),
                member -> reject(member));
        rvRequests.setAdapter(adapter);
    }

    private void approve(ClassMemberResponse member) {
        apiService.approveRequest(classId, member.getId()).enqueue(new Callback<ApiResponse<Void>>() {
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> r) {
                Toast.makeText(ApproveRequestsActivity.this,
                        r.isSuccessful() ? "Approved" : "Error", Toast.LENGTH_SHORT).show();
                loadRequests();
            }
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(ApproveRequestsActivity.this, "Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void reject(ClassMemberResponse member) {
        apiService.rejectRequest(classId, member.getId()).enqueue(new Callback<ApiResponse<Void>>() {
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> r) {
                Toast.makeText(ApproveRequestsActivity.this,
                        r.isSuccessful() ? "Rejected" : "Error", Toast.LENGTH_SHORT).show();
                loadRequests();
            }
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(ApproveRequestsActivity.this, "Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}