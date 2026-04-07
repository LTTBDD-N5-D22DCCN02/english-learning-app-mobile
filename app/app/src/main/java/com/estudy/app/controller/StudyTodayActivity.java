package com.estudy.app.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.estudy.app.R;
import com.estudy.app.api.ApiClient;
import com.estudy.app.api.ApiService;
import com.estudy.app.model.response.*;
import com.estudy.app.utils.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.*;

/**
 * UC-STUDY-01: "Study Today" screen.
 * Accessible ONLY from HomeActivity — NOT part of the bottom nav.
 */
public class StudyTodayActivity extends AppCompatActivity {

    private TextView tvSubtitle, tvDueCount, tvNewCount, tvDoneCount;
    private TextView tvDueTotalLabel, tvNewTotalLabel;
    private Button btnStudyAll;
    private RecyclerView rvDueSets, rvNewSets;
    private LinearLayout layoutDueSection, layoutNewSection, layoutEmpty;
    private ProgressBar progressBar;

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_today);

        TokenManager tm = new TokenManager(this);
        apiService = ApiClient.getInstance(tm).create(ApiService.class);

        setupToolbar();
        bindViews();
        loadStudyToday();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStudyToday();
    }

    private void setupToolbar() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        TextView tvTitle = findViewById(R.id.tvToolbarTitle);
        if (tvTitle != null) tvTitle.setText("Study today");
    }

    private void bindViews() {
        tvSubtitle       = findViewById(R.id.tvSubtitle);
        tvDueCount       = findViewById(R.id.tvDueCount);
        tvNewCount       = findViewById(R.id.tvNewCount);
        tvDoneCount      = findViewById(R.id.tvDoneCount);
        tvDueTotalLabel  = findViewById(R.id.tvDueTotalLabel);
        tvNewTotalLabel  = findViewById(R.id.tvNewTotalLabel);
        btnStudyAll      = findViewById(R.id.btnStudyAll);
        rvDueSets        = findViewById(R.id.rvDueSets);
        rvNewSets        = findViewById(R.id.rvNewSets);
        layoutDueSection = findViewById(R.id.layoutDueSection);
        layoutNewSection = findViewById(R.id.layoutNewSection);
        layoutEmpty      = findViewById(R.id.layoutEmpty);
        progressBar      = findViewById(R.id.progressBar);

        if (rvDueSets != null) {
            rvDueSets.setLayoutManager(new LinearLayoutManager(this));
            rvDueSets.setNestedScrollingEnabled(false);
        }
        if (rvNewSets != null) {
            rvNewSets.setLayoutManager(new LinearLayoutManager(this));
            rvNewSets.setNestedScrollingEnabled(false);
        }

        View btnDueMode = findViewById(R.id.btnDueMode);
        if (btnDueMode != null)
            btnDueMode.setOnClickListener(v -> showModeBottomSheet("", "Due today - All sets"));
    }

    private void loadStudyToday() {
        showLoading(true);
        apiService.getStudyToday().enqueue(new Callback<ApiResponse<StudyTodayResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<StudyTodayResponse>> call,
                                   Response<ApiResponse<StudyTodayResponse>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null
                        && response.body().getResult() != null) {
                    bindStudyData(response.body().getResult());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<StudyTodayResponse>> call, Throwable t) {
                showLoading(false);
            }
        });
    }

    private void bindStudyData(StudyTodayResponse data) {
        int totalDue = data.getTotalDue();
        int totalNew = data.getTotalNew();
        int total    = totalDue + totalNew;

        if (tvDueCount  != null) tvDueCount.setText(String.valueOf(totalDue));
        if (tvNewCount  != null) tvNewCount.setText(String.valueOf(totalNew));
        if (tvDoneCount != null) tvDoneCount.setText("0");
        if (tvSubtitle  != null) tvSubtitle.setText(total + " words · " + countSets(data) + " sets");

        if (btnStudyAll != null) {
            btnStudyAll.setText("Study all (" + total + ")");
            btnStudyAll.setVisibility(total > 0 ? View.VISIBLE : View.GONE);
            btnStudyAll.setOnClickListener(v -> showModeBottomSheet("", "All sets"));
        }

        // Due section
        List<StudySetItem> dueSets = data.getDueSets();
        if (dueSets != null && !dueSets.isEmpty()) {
            if (layoutDueSection != null) layoutDueSection.setVisibility(View.VISIBLE);
            if (tvDueTotalLabel  != null) tvDueTotalLabel.setText(totalDue + " words");
            if (rvDueSets != null)
                rvDueSets.setAdapter(new StudySetAdapter(dueSets, true,
                        item -> showModeBottomSheet(item.getSetId(), item.getSetName())));
        } else {
            if (layoutDueSection != null) layoutDueSection.setVisibility(View.GONE);
        }

        // New section
        List<StudySetItem> newSets = data.getNewSets();
        if (newSets != null && !newSets.isEmpty()) {
            if (layoutNewSection != null) layoutNewSection.setVisibility(View.VISIBLE);
            if (tvNewTotalLabel  != null) tvNewTotalLabel.setText(totalNew + " words");
            if (rvNewSets != null)
                rvNewSets.setAdapter(new StudySetAdapter(newSets, false,
                        item -> showModeBottomSheet(item.getSetId(), item.getSetName())));
        } else {
            if (layoutNewSection != null) layoutNewSection.setVisibility(View.GONE);
        }

        if (layoutEmpty != null)
            layoutEmpty.setVisibility(total == 0 ? View.VISIBLE : View.GONE);
    }

    private void showModeBottomSheet(String setId, String setName) {
        StudyModeBottomSheet sheet = StudyModeBottomSheet.newInstance(setId, setName);
        sheet.show(getSupportFragmentManager(), "StudyMode");
    }

    private int countSets(StudyTodayResponse data) {
        Set<String> ids = new HashSet<>();
        if (data.getDueSets() != null)
            for (StudySetItem s : data.getDueSets()) ids.add(s.getSetId());
        if (data.getNewSets() != null)
            for (StudySetItem s : data.getNewSets()) ids.add(s.getSetId());
        return ids.size();
    }

    private void showLoading(boolean loading) {
        if (progressBar != null)
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
