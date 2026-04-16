package com.estudy.app.controller;

import android.app.AlertDialog;
import android.content.Intent;
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

public class MyClassesActivity extends AppCompatActivity {

    private RecyclerView rvClasses;
    private TextView tvEmpty;
    private EditText etSearch;
    private ImageButton btnAdd;
    private ApiService apiService;
    private TokenManager tokenManager;
    private List<ClassResponse> classList = new ArrayList<>();
    private ClassAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_classes);
        setupBottomNav();

        tokenManager = new TokenManager(this);
        apiService = ApiClient.getInstance(tokenManager).create(ApiService.class);

        rvClasses = findViewById(R.id.rvClasses);
        tvEmpty = findViewById(R.id.tvEmpty);
        etSearch = findViewById(R.id.etSearch);
        btnAdd = findViewById(R.id.btnAdd);

        rvClasses.setLayoutManager(new LinearLayoutManager(this));

        btnAdd.setOnClickListener(v -> showAddOptions());

        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchClasses(s.toString().trim());
            }
            public void afterTextChanged(Editable s) {}
        });

        loadMyClasses();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMyClasses();
    }

    private void showAddOptions() {
        String[] options = {"New Class", "Join a class"};
        new AlertDialog.Builder(this)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        startActivity(new Intent(this, CreateClassActivity.class));
                    } else {
                        showJoinDialog();
                    }
                }).show();
    }

    private void showJoinDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_join_class, null);
        EditText etCode = view.findViewById(R.id.etClassCode);
        new AlertDialog.Builder(this)
                .setTitle("Join a class")
                .setView(view)
                .setPositiveButton("Join", (d, w) -> {
                    String code = etCode.getText().toString().trim();
                    if (code.isEmpty()) {
                        Toast.makeText(this, "Please enter class code", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    joinClass(code);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void joinClass(String code) {
        // Truyền thẳng code vào constructor — sẽ serialize thành {"code": "..."}
        apiService.joinClass(new com.estudy.app.model.request.JoinClassRequest(code))
                .enqueue(new Callback<ApiResponse<Void>>() {
                    public void onResponse(Call<ApiResponse<Void>> call,
                                           Response<ApiResponse<Void>> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(MyClassesActivity.this,
                                    "Request sent! Waiting for approval.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            // Parse lỗi cụ thể từ backend
                            String msg;
                            switch (response.code()) {
                                case 400: msg = "Invalid class code"; break;
                                case 409: msg = "You already sent a request or are a member"; break;
                                case 404: msg = "Class not found"; break;
                                default:  msg = "Error: " + response.code();
                            }
                            Toast.makeText(MyClassesActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    }
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        Toast.makeText(MyClassesActivity.this,
                                "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadMyClasses() {
        apiService.getMyClasses().enqueue(new Callback<ApiResponse<List<ClassResponse>>>() {
            public void onResponse(Call<ApiResponse<List<ClassResponse>>> call,
                                   Response<ApiResponse<List<ClassResponse>>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getResult() != null) {
                    classList = response.body().getResult();
                    setupAdapter(classList);
                }
            }
            public void onFailure(Call<ApiResponse<List<ClassResponse>>> call, Throwable t) {
                Toast.makeText(MyClassesActivity.this, "Load failed: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchClasses(String keyword) {
        if (keyword.isEmpty()) {
            setupAdapter(classList);
            return;
        }
        apiService.searchMyClasses(keyword).enqueue(new Callback<ApiResponse<List<ClassResponse>>>() {
            public void onResponse(Call<ApiResponse<List<ClassResponse>>> call,
                                   Response<ApiResponse<List<ClassResponse>>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getResult() != null) {
                    setupAdapter(response.body().getResult());
                }
            }
            public void onFailure(Call<ApiResponse<List<ClassResponse>>> call, Throwable t) {}
        });
    }

    private void setupAdapter(List<ClassResponse> list) {
        if (list == null || list.isEmpty()) {
            rvClasses.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            rvClasses.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
        }
        adapter = new ClassAdapter(list, this::onClassClick, this::onClassMenuClick);
        rvClasses.setAdapter(adapter);
    }

    private void onClassClick(ClassResponse cls) {
        Intent intent = new Intent(this, ClassDetailActivity.class);
        intent.putExtra("classId", cls.getId());
        intent.putExtra("className", cls.getName());
        intent.putExtra("myRole", cls.getMyRole());
        startActivity(intent);
    }

    private void onClassMenuClick(ClassResponse cls, View anchor) {
        boolean isLeader = "LEADER".equals(cls.getMyRole());
        if (isLeader) {
            String[] opts = {"Edit", "Delete"};
            new AlertDialog.Builder(this)
                    .setItems(opts, (d, w) -> {
                        if (w == 0) {
                            Intent i = new Intent(this, CreateClassActivity.class);
                            i.putExtra("edit", true);
                            i.putExtra("classId", cls.getId());
                            i.putExtra("name", cls.getName());
                            i.putExtra("description", cls.getDescription());
                            i.putExtra("privacy", cls.getPrivacy());
                            startActivity(i);
                        } else {
                            confirmDeleteClass(cls);
                        }
                    }).show();
        } else {
            new AlertDialog.Builder(this)
                    .setItems(new String[]{"Exit Group"}, (d, w) -> confirmLeaveClass(cls))
                    .show();
        }
    }

    private void confirmDeleteClass(ClassResponse cls) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Class")
                .setMessage("Are you sure you want to delete \"" + cls.getName() + "\"?")
                .setPositiveButton("Delete", (d, w) -> {
                    apiService.deleteClass(cls.getId()).enqueue(new Callback<ApiResponse<Void>>() {
                        public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> r) {
                            if (r.isSuccessful()) {
                                Toast.makeText(MyClassesActivity.this, "Class deleted", Toast.LENGTH_SHORT).show();
                                loadMyClasses();
                            }
                        }
                        public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                            Toast.makeText(MyClassesActivity.this, "Error", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void confirmLeaveClass(ClassResponse cls) {
        new AlertDialog.Builder(this)
                .setTitle("Exit Group")
                .setMessage("Are you sure you want to leave \"" + cls.getName() + "\"?")
                .setPositiveButton("Exit", (d, w) -> {
                    apiService.leaveClass(cls.getId()).enqueue(new Callback<ApiResponse<Void>>() {
                        public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> r) {
                            if (r.isSuccessful()) {
                                Toast.makeText(MyClassesActivity.this, "Left class", Toast.LENGTH_SHORT).show();
                                loadMyClasses();
                            }
                        }
                        public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                            Toast.makeText(MyClassesActivity.this, "Error", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null).show();
    }
    private void setupBottomNav() {
        View btnNavHome    = findViewById(R.id.btnNavHome);
        View btnNavSets    = findViewById(R.id.btnNavSets);
        View btnNavAdd     = findViewById(R.id.btnNavAdd);
        View btnNavClasses = findViewById(R.id.btnNavClasses);

        // Highlight tab Classes (đang active)
        if (btnNavClasses != null) {
            btnNavClasses.setBackground(
                    getResources().getDrawable(R.drawable.bg_nav_active, null));
        }

        if (btnNavHome != null)
            btnNavHome.setOnClickListener(v -> {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            });
        if (btnNavSets != null)
            btnNavSets.setOnClickListener(v ->
                    startActivity(new Intent(this, FlashCardSetListActivity.class)));
        if (btnNavAdd != null)
            btnNavAdd.setOnClickListener(v ->
                    startActivity(new Intent(this, FlashCardSetCreateActivity.class)));
        if (btnNavClasses != null)
            btnNavClasses.setOnClickListener(v -> { /* đang ở đây */ });
    }
}