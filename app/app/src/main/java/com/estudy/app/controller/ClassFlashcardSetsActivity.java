package com.estudy.app.controller;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.estudy.app.R;
import com.estudy.app.api.ApiClient;
import com.estudy.app.api.ApiService;
import com.estudy.app.model.request.ClassFlashCardSetRequest;
import com.estudy.app.model.response.ApiResponse;
import com.estudy.app.model.response.FlashCardSetResponse;
import com.estudy.app.utils.TokenManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClassFlashcardSetsActivity extends AppCompatActivity {

    private static final String TAG = "ClassFlashcardSets";

    private RecyclerView rvFlashcardSets;
    private TextView tvEmpty;
    private EditText etSearch;
    private ImageButton btnSort;
    private ImageButton btnAdd;
    private ApiService apiService;
    private TokenManager tokenManager;
    private String classId, myRole, currentUserId;
    private List<FlashCardSetResponse> setList = new ArrayList<>();
    private boolean sortAscending = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_flashcard_sets);

        classId = getIntent().getStringExtra("classId");
        myRole  = getIntent().getStringExtra("myRole");

        tokenManager  = new TokenManager(this);
        apiService    = ApiClient.getInstance(tokenManager).create(ApiService.class);
        currentUserId = tokenManager.getCurrentUsername();

        View toolbar = findViewById(R.id.toolbar);
        ((ImageButton) toolbar.findViewById(R.id.btnBack)).setOnClickListener(v -> finish());
        ((TextView) toolbar.findViewById(R.id.tvToolbarTitle)).setText("Flashcard Sets");

        rvFlashcardSets = findViewById(R.id.rvFlashcardSets);
        tvEmpty         = findViewById(R.id.tvEmpty);
        etSearch        = findViewById(R.id.etSearch);
        btnSort         = findViewById(R.id.btnSort);
        btnAdd          = findViewById(R.id.btnAdd);

        rvFlashcardSets.setLayoutManager(new LinearLayoutManager(this));

        // Fix #4: Nút sort theo alphabet
        btnSort.setOnClickListener(v -> {
            sortAscending = !sortAscending;
            sortAndRefresh();
            Toast.makeText(this,
                    "Sorted: " + (sortAscending ? "A → Z" : "Z → A"),
                    Toast.LENGTH_SHORT).show();
        });

        btnAdd.setOnClickListener(v -> showCreateDialog());

        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                String kw = s.toString().trim();
                if (kw.isEmpty()) setupAdapter(setList);
                else searchSets(kw);
            }
            public void afterTextChanged(Editable s) {}
        });

        loadSets();
    }

    @Override
    protected void onResume() { super.onResume(); loadSets(); }

    private void sortAndRefresh() {
        if (setList == null || setList.isEmpty()) return;
        List<FlashCardSetResponse> sorted = new ArrayList<>(setList);
        if (sortAscending) {
            Collections.sort(sorted, (a, b) ->
                    a.getName().compareToIgnoreCase(b.getName()));
        } else {
            Collections.sort(sorted, (a, b) ->
                    b.getName().compareToIgnoreCase(a.getName()));
        }
        setupAdapter(sorted);
    }

    private void loadSets() {
        apiService.getClassFlashCardSets(classId)
                .enqueue(new Callback<ApiResponse<List<FlashCardSetResponse>>>() {
                    public void onResponse(Call<ApiResponse<List<FlashCardSetResponse>>> call,
                                           Response<ApiResponse<List<FlashCardSetResponse>>> r) {
                        if (r.isSuccessful() && r.body() != null
                                && r.body().getResult() != null) {
                            setList = r.body().getResult();
                            setupAdapter(setList);
                        } else {
                            Log.e(TAG, "loadSets error: " + r.code());
                        }
                    }
                    public void onFailure(Call<ApiResponse<List<FlashCardSetResponse>>> call,
                                          Throwable t) {
                        Log.e(TAG, "loadSets failure: " + t.getMessage());
                        Toast.makeText(ClassFlashcardSetsActivity.this,
                                "Load failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void searchSets(String keyword) {
        apiService.searchClassFlashCardSets(classId, keyword)
                .enqueue(new Callback<ApiResponse<List<FlashCardSetResponse>>>() {
                    public void onResponse(Call<ApiResponse<List<FlashCardSetResponse>>> call,
                                           Response<ApiResponse<List<FlashCardSetResponse>>> r) {
                        if (r.isSuccessful() && r.body() != null && r.body().getResult() != null)
                            setupAdapter(r.body().getResult());
                    }
                    public void onFailure(Call<ApiResponse<List<FlashCardSetResponse>>> call,
                                          Throwable t) {}
                });
    }

    private void setupAdapter(List<FlashCardSetResponse> list) {
        if (list == null || list.isEmpty()) {
            rvFlashcardSets.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            rvFlashcardSets.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
        }
        boolean isLeader = "LEADER".equals(myRole);
        ClassFlashcardSetAdapter adapter = new ClassFlashcardSetAdapter(
                list, isLeader, currentUserId, this::onSetMenu);
        adapter.setOnClickListener(this::openSetDetail);
        rvFlashcardSets.setAdapter(adapter);
    }

    private void onSetMenu(FlashCardSetResponse set) {
        boolean isLeader = "LEADER".equals(myRole);
        boolean isOwner  = set.getOwnerId() != null
                && set.getOwnerId().equals(currentUserId);
        if (!isLeader && !isOwner) return;

        new AlertDialog.Builder(this)
                .setItems(new String[]{"Edit", "Delete"}, (d, w) -> {
                    if (w == 0) showEditDialog(set);
                    else confirmDelete(set);
                }).show();
    }

    // Click vào set → mở FlashCardListActivity với classId
    private void openSetDetail(FlashCardSetResponse set) {
        android.content.Intent i = new android.content.Intent(
                this, FlashCardListActivity.class);
        i.putExtra("flashcard_set_id", set.getId());
        i.putExtra("flashcard_set_name", set.getName());
        i.putExtra("class_id", classId);  // Truyền classId để load từ endpoint class
        startActivity(i);
    }

    private void showCreateDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_flashcard_set_form, null);
        EditText etName   = view.findViewById(R.id.etFsName);
        EditText etDesc   = view.findViewById(R.id.etFsDescription);
        Spinner  spinner  = view.findViewById(R.id.spinnerFsPrivacy);
        setupPrivacySpinner(spinner);

        new AlertDialog.Builder(this)
                .setTitle("New Flashcard Set")
                .setView(view)
                .setPositiveButton("Create", (d, w) -> {
                    String name    = etName.getText().toString().trim();
                    String desc    = etDesc.getText().toString().trim();
                    String privacy = spinner.getSelectedItemPosition() == 1 ? "PUBLIC" : "PRIVATE";
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    apiService.addClassFlashCardSet(classId,
                                    new ClassFlashCardSetRequest(name, desc, privacy))
                            .enqueue(new Callback<ApiResponse<FlashCardSetResponse>>() {
                                public void onResponse(
                                        Call<ApiResponse<FlashCardSetResponse>> call,
                                        Response<ApiResponse<FlashCardSetResponse>> r) {
                                    Toast.makeText(ClassFlashcardSetsActivity.this,
                                            r.isSuccessful() ? "Created!" : "Error " + r.code(),
                                            Toast.LENGTH_SHORT).show();
                                    if (r.isSuccessful()) loadSets();
                                }
                                public void onFailure(
                                        Call<ApiResponse<FlashCardSetResponse>> call, Throwable t) {
                                    Toast.makeText(ClassFlashcardSetsActivity.this,
                                            "Network error", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void showEditDialog(FlashCardSetResponse set) {
        View view    = LayoutInflater.from(this).inflate(R.layout.dialog_flashcard_set_form, null);
        EditText etName  = view.findViewById(R.id.etFsName);
        EditText etDesc  = view.findViewById(R.id.etFsDescription);
        Spinner  spinner = view.findViewById(R.id.spinnerFsPrivacy);
        setupPrivacySpinner(spinner);
        etName.setText(set.getName());
        etDesc.setText(set.getDescription());
        spinner.setSelection("PUBLIC".equalsIgnoreCase(set.getPrivacy()) ? 1 : 0);

        new AlertDialog.Builder(this)
                .setTitle("Edit Flashcard Set")
                .setView(view)
                .setPositiveButton("Save", (d, w) -> {
                    String name    = etName.getText().toString().trim();
                    String desc    = etDesc.getText().toString().trim();
                    String privacy = spinner.getSelectedItemPosition() == 1 ? "PUBLIC" : "PRIVATE";
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Fix #2: đảm bảo set.getId() không null
                    if (set.getId() == null) {
                        Toast.makeText(this, "Invalid set ID", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    apiService.updateClassFlashCardSet(classId, set.getId(),
                                    new ClassFlashCardSetRequest(name, desc, privacy))
                            .enqueue(new Callback<ApiResponse<FlashCardSetResponse>>() {
                                public void onResponse(
                                        Call<ApiResponse<FlashCardSetResponse>> call,
                                        Response<ApiResponse<FlashCardSetResponse>> r) {
                                    if (r.isSuccessful()) {
                                        Toast.makeText(ClassFlashcardSetsActivity.this,
                                                "Updated", Toast.LENGTH_SHORT).show();
                                        loadSets();
                                    } else {
                                        // Log để debug
                                        Log.e(TAG, "Update error: " + r.code());
                                        try {
                                            Log.e(TAG, "Body: " + r.errorBody().string());
                                        } catch (Exception ignored) {}
                                        Toast.makeText(ClassFlashcardSetsActivity.this,
                                                "Update failed: " + r.code(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                                public void onFailure(
                                        Call<ApiResponse<FlashCardSetResponse>> call, Throwable t) {
                                    Log.e(TAG, "Update failure: " + t.getMessage());
                                    Toast.makeText(ClassFlashcardSetsActivity.this,
                                            "Network error", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void confirmDelete(FlashCardSetResponse set) {
        new AlertDialog.Builder(this)
                .setTitle("Delete")
                .setMessage("Delete \"" + set.getName() + "\"?")
                .setPositiveButton("Delete", (d, w) -> {
                    if (set.getId() == null) {
                        Toast.makeText(this, "Invalid set ID", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    apiService.deleteClassFlashCardSet(classId, set.getId())
                            .enqueue(new Callback<ApiResponse<Void>>() {
                                public void onResponse(Call<ApiResponse<Void>> call,
                                                       Response<ApiResponse<Void>> r) {
                                    if (r.isSuccessful()) {
                                        Toast.makeText(ClassFlashcardSetsActivity.this,
                                                "Deleted", Toast.LENGTH_SHORT).show();
                                        loadSets();
                                    } else {
                                        Log.e(TAG, "Delete error: " + r.code());
                                        Toast.makeText(ClassFlashcardSetsActivity.this,
                                                "Delete failed: " + r.code(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                                    Log.e(TAG, "Delete failure: " + t.getMessage());
                                    Toast.makeText(ClassFlashcardSetsActivity.this,
                                            "Network error", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void setupPrivacySpinner(Spinner spinner) {
        ArrayAdapter<String> a = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"Private", "Public"});
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(a);
    }
}