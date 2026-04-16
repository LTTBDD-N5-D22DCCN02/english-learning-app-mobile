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
import com.estudy.app.model.request.CopyClassRequest;
import com.estudy.app.model.request.UpdateMemberRoleRequest;
import com.estudy.app.model.response.ApiResponse;
import com.estudy.app.model.response.ClassMemberResponse;
import com.estudy.app.model.response.ClassResponse;
import com.estudy.app.utils.TokenManager;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MemberListActivity extends AppCompatActivity {

    private RecyclerView rvMembers;
    private EditText etSearch;
    private ImageButton btnBack, btnMore;
    private ApiService apiService;
    private TokenManager tokenManager;
    private String classId, myRole;
    private List<ClassMemberResponse> memberList = new ArrayList<>();
    private MemberAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_list);

        classId = getIntent().getStringExtra("classId");
        myRole = getIntent().getStringExtra("myRole");

        tokenManager = new TokenManager(this);
        apiService = ApiClient.getInstance(tokenManager).create(ApiService.class);

        View toolbar = findViewById(R.id.toolbar);
        btnBack = toolbar.findViewById(R.id.btnBack);
        TextView tvTitle = toolbar.findViewById(R.id.tvToolbarTitle);
        tvTitle.setText("Member list");
        btnBack.setOnClickListener(v -> finish());

        rvMembers = findViewById(R.id.rvMembers);
        etSearch = findViewById(R.id.etSearch);
        btnMore = findViewById(R.id.btnMore);
        rvMembers.setLayoutManager(new LinearLayoutManager(this));

        // UC-09: Copy class (chỉ member)
        if (!"LEADER".equals(myRole)) {
            btnMore.setOnClickListener(v -> showCopyClassDialog());
        } else {
            btnMore.setVisibility(View.GONE);
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                searchMembers(s.toString().trim());
            }
            public void afterTextChanged(Editable s) {}
        });

        loadMembers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMembers();
    }

    private void loadMembers() {
        apiService.getMembers(classId).enqueue(new Callback<ApiResponse<List<ClassMemberResponse>>>() {
            public void onResponse(Call<ApiResponse<List<ClassMemberResponse>>> call,
                                   Response<ApiResponse<List<ClassMemberResponse>>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getResult() != null) {
                    memberList = response.body().getResult();
                    setupAdapter(memberList);
                }
            }
            public void onFailure(Call<ApiResponse<List<ClassMemberResponse>>> call, Throwable t) {
                Toast.makeText(MemberListActivity.this, "Load failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchMembers(String keyword) {
        if (keyword.isEmpty()) { setupAdapter(memberList); return; }
        apiService.searchMembers(classId, keyword).enqueue(new Callback<ApiResponse<List<ClassMemberResponse>>>() {
            public void onResponse(Call<ApiResponse<List<ClassMemberResponse>>> call,
                                   Response<ApiResponse<List<ClassMemberResponse>>> r) {
                if (r.isSuccessful() && r.body() != null && r.body().getResult() != null) {
                    setupAdapter(r.body().getResult());
                }
            }
            public void onFailure(Call<ApiResponse<List<ClassMemberResponse>>> call, Throwable t) {}
        });
    }

    private void setupAdapter(List<ClassMemberResponse> list) {
        boolean isLeader = "LEADER".equals(myRole);
        adapter = new MemberAdapter(list, isLeader, this::onMemberMenu);
        rvMembers.setAdapter(adapter);
    }

    private void onMemberMenu(ClassMemberResponse member) {
        if (!"LEADER".equals(myRole)) return;
        String[] opts = {"Modify permissions", "Delete"};
        new AlertDialog.Builder(this)
                .setItems(opts, (d, w) -> {
                    if (w == 0) showRoleDialog(member);
                    else confirmRemoveMember(member);
                }).show();
    }

    private void showRoleDialog(ClassMemberResponse member) {
        String[] roles = {"Leader", "Member"};
        new AlertDialog.Builder(this)
                .setTitle("Modify permissions")
                .setItems(roles, (d, w) -> {
                    String newRole = w == 0 ? "LEADER" : "MEMBER";
                    apiService.updateMemberRole(classId, member.getUserId(),
                                    new UpdateMemberRoleRequest(newRole))
                            .enqueue(new Callback<ApiResponse<Void>>() {
                                public void onResponse(Call<ApiResponse<Void>> call,
                                                       Response<ApiResponse<Void>> r) {
                                    Toast.makeText(MemberListActivity.this,
                                            r.isSuccessful() ? "Role updated" : "Error",
                                            Toast.LENGTH_SHORT).show();
                                    loadMembers();
                                }
                                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                                    Toast.makeText(MemberListActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                }
                            });
                }).show();
    }

    private void confirmRemoveMember(ClassMemberResponse member) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Member")
                .setMessage("Remove " + member.getFullName() + " from class?")
                .setPositiveButton("Delete", (d, w) -> {
                    apiService.removeMember(classId, member.getUserId())
                            .enqueue(new Callback<ApiResponse<Void>>() {
                                public void onResponse(Call<ApiResponse<Void>> call,
                                                       Response<ApiResponse<Void>> r) {
                                    Toast.makeText(MemberListActivity.this,
                                            r.isSuccessful() ? "Removed" : "Error",
                                            Toast.LENGTH_SHORT).show();
                                    loadMembers();
                                }
                                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                                    Toast.makeText(MemberListActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void showCopyClassDialog() {
        // UC-09: Copy class
        View view = getLayoutInflater().inflate(R.layout.dialog_copy_class, null);
        EditText etName = view.findViewById(R.id.etNewClassName);
        new AlertDialog.Builder(this)
                .setTitle("Copy class")
                .setView(view)
                .setPositiveButton("Copy", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Please enter class name", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    apiService.copyClass(classId, new CopyClassRequest(name))
                            .enqueue(new Callback<ApiResponse<ClassResponse>>() {
                                public void onResponse(Call<ApiResponse<ClassResponse>> call,
                                                       Response<ApiResponse<ClassResponse>> r) {
                                    if (r.isSuccessful() && r.body() != null) {
                                        Toast.makeText(MemberListActivity.this,
                                                "Class copied!", Toast.LENGTH_SHORT).show();
                                        // Navigate to new class
                                        ClassResponse newCls = r.body().getResult();
                                        if (newCls != null) {
                                            Intent i = new Intent(MemberListActivity.this, ClassDetailActivity.class);
                                            i.putExtra("classId", newCls.getId());
                                            i.putExtra("className", newCls.getName());
                                            i.putExtra("myRole", newCls.getMyRole());
                                            startActivity(i);
                                        }
                                    } else {
                                        Toast.makeText(MemberListActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                public void onFailure(Call<ApiResponse<ClassResponse>> call, Throwable t) {
                                    Toast.makeText(MemberListActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null).show();
    }
}