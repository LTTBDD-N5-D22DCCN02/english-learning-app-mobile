package com.estudy.app.controller;

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
import com.estudy.app.utils.BottomNavHelper;
import com.estudy.app.utils.StudyBarChartView;
import com.estudy.app.utils.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.*;

public class StatisticsActivity extends AppCompatActivity {

    private TextView tvCurrentStreak, tvLongestStreak;
    private TextView tvWordsLearned, tvWordsMastered;
    private TextView tvTotalAnswers, tvAccuracy;
    private TextView btnWeekly, btnMonthly;
    private StudyBarChartView barChart;
    private RecyclerView rvSetProgress;
    private LinearLayout layoutStatEmpty, badgeNewRecord;
    private TextView tvAvgAccuracy, tvAccuracyTrendIcon, tvAccuracyTrend;
    private ProgressBar progressBarStat;

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        BottomNavHelper.setup(this, R.id.btnNavStats);

        TokenManager tm = new TokenManager(this);
        apiService = ApiClient.getInstance(tm).create(ApiService.class);

        setupToolbar();
        bindViews();
        loadStatistics("weekly");
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStatistics("weekly");
    }

    private void setupToolbar() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void bindViews() {
        tvCurrentStreak     = findViewById(R.id.tvCurrentStreak);
        tvLongestStreak     = findViewById(R.id.tvLongestStreak);
        tvWordsLearned      = findViewById(R.id.tvWordsLearned);
        tvWordsMastered     = findViewById(R.id.tvWordsMastered);
        tvTotalAnswers      = findViewById(R.id.tvTotalAnswers);
        tvAccuracy          = findViewById(R.id.tvAccuracy);
        btnWeekly           = findViewById(R.id.btnWeekly);
        btnMonthly          = findViewById(R.id.btnMonthly);
        barChart            = findViewById(R.id.barChart);
        rvSetProgress       = findViewById(R.id.rvSetProgress);
        layoutStatEmpty     = findViewById(R.id.layoutStatEmpty);
        progressBarStat     = findViewById(R.id.progressBarStat);
        badgeNewRecord      = findViewById(R.id.badgeNewRecord);
        tvAvgAccuracy       = findViewById(R.id.tvAvgAccuracy);
        tvAccuracyTrendIcon = findViewById(R.id.tvAccuracyTrendIcon);
        tvAccuracyTrend     = findViewById(R.id.tvAccuracyTrend);

        if (rvSetProgress != null) {
            rvSetProgress.setLayoutManager(new LinearLayoutManager(this));
            rvSetProgress.setNestedScrollingEnabled(false);
        }

        if (btnWeekly != null)
            btnWeekly.setOnClickListener(v -> {
                setPeriodSelected(true);
                loadActivityChart("weekly");
            });
        if (btnMonthly != null)
            btnMonthly.setOnClickListener(v -> {
                setPeriodSelected(false);
                loadActivityChart("monthly");
            });
    }

    private void loadStatistics(String period) {
        loadStatSummary();
        loadActivityChart(period);
        loadSetProgress();
        setPeriodSelected(period.equals("weekly"));
    }

    private void loadStatSummary() {
        if (progressBarStat != null) progressBarStat.setVisibility(View.VISIBLE);

        apiService.getStatSummary().enqueue(new Callback<ApiResponse<StatSummaryResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<StatSummaryResponse>> call,
                                   Response<ApiResponse<StatSummaryResponse>> response) {
                if (progressBarStat != null) progressBarStat.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null
                        && response.body().getResult() != null) {
                    bindStatSummary(response.body().getResult());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<StatSummaryResponse>> call, Throwable t) {
                if (progressBarStat != null) progressBarStat.setVisibility(View.GONE);
            }
        });
    }

    private void bindStatSummary(StatSummaryResponse stat) {
        if (tvCurrentStreak != null)
            tvCurrentStreak.setText(stat.getCurrentStreak() + " ngày liên tiếp");
        if (tvLongestStreak != null)
            tvLongestStreak.setText("Kỷ lục: " + stat.getLongestStreak() + " ngày");
        if (tvWordsLearned  != null)
            tvWordsLearned.setText(String.valueOf(stat.getWordsLearned()));
        if (tvWordsMastered != null)
            tvWordsMastered.setText(String.valueOf(stat.getWordsMastered()));
        if (tvTotalAnswers  != null)
            tvTotalAnswers.setText(String.valueOf(stat.getTotalAnswers()));
        if (tvAccuracy != null)
            tvAccuracy.setText(String.format("%.0f%%", stat.getAccuracyPercent()));

        // UC-STAT-03: show new record badge when streak hits a new high
        if (badgeNewRecord != null)
            badgeNewRecord.setVisibility(stat.isNewRecord() ? View.VISIBLE : View.GONE);

        boolean hasData = stat.getTotalAnswers() > 0 || stat.getWordsLearned() > 0;
        if (layoutStatEmpty != null)
            layoutStatEmpty.setVisibility(hasData ? View.GONE : View.VISIBLE);
    }

    private void loadActivityChart(String period) {
        apiService.getStudyActivity(period)
                .enqueue(new Callback<ApiResponse<List<DayActivityResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<DayActivityResponse>>> call,
                                   Response<ApiResponse<List<DayActivityResponse>>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getResult() != null) {
                    List<DayActivityResponse> data = response.body().getResult();
                    if (barChart != null) barChart.setData(data);
                    computeAndShowTrend(data);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<DayActivityResponse>>> call, Throwable t) {}
        });
    }

    private void computeAndShowTrend(List<DayActivityResponse> data) {
        if (data == null || data.isEmpty()) return;

        List<Double> accuracies = new ArrayList<>();
        for (DayActivityResponse day : data) {
            if (day.getWordCount() > 0) {
                accuracies.add(day.getAccuracyPercent());
            }
        }

        if (accuracies.isEmpty()) return;

        double total = 0;
        for (double a : accuracies) total += a;
        double avg = total / accuracies.size();

        if (tvAvgAccuracy != null)
            tvAvgAccuracy.setText(String.format("%.0f%%", avg));

        if (accuracies.size() < 2) {
            if (tvAccuracyTrendIcon != null) tvAccuracyTrendIcon.setText("➡️");
            if (tvAccuracyTrend     != null) tvAccuracyTrend.setText("Chưa đủ dữ liệu");
            return;
        }

        int mid = accuracies.size() / 2;
        double sumFirst = 0, sumSecond = 0;
        for (int i = 0; i < mid; i++) sumFirst += accuracies.get(i);
        for (int i = mid; i < accuracies.size(); i++) sumSecond += accuracies.get(i);
        double avgFirst  = sumFirst  / mid;
        double avgSecond = sumSecond / (accuracies.size() - mid);
        double delta = avgSecond - avgFirst;

        String icon, label;
        if (delta > 3) {
            icon  = "📈";
            label = String.format("+%.0f%% so với trước", delta);
        } else if (delta < -3) {
            icon  = "📉";
            label = String.format("%.0f%% so với trước", delta);
        } else {
            icon  = "➡️";
            label = "Ổn định";
        }

        if (tvAccuracyTrendIcon != null) tvAccuracyTrendIcon.setText(icon);
        if (tvAccuracyTrend     != null) tvAccuracyTrend.setText(label);
    }

    private void loadSetProgress() {
        apiService.getSetProgress()
                .enqueue(new Callback<ApiResponse<List<SetProgressResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<SetProgressResponse>>> call,
                                   Response<ApiResponse<List<SetProgressResponse>>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getResult() != null && rvSetProgress != null) {
                    List<SetProgressResponse> list = response.body().getResult();
                    rvSetProgress.setAdapter(new SetProgressAdapter(list));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<SetProgressResponse>>> call, Throwable t) {}
        });
    }

    private void setPeriodSelected(boolean weekly) {
        if (btnWeekly != null) {
            btnWeekly.setBackgroundResource(weekly ?
                    R.drawable.bg_tab_selected_small : android.R.color.transparent);
            btnWeekly.setTextColor(weekly ? 0xFFFFFFFF : 0xFF005AAE);
        }
        if (btnMonthly != null) {
            btnMonthly.setBackgroundResource(weekly ?
                    android.R.color.transparent : R.drawable.bg_tab_selected_small);
            btnMonthly.setTextColor(weekly ? 0xFF005AAE : 0xFFFFFFFF);
        }
    }
}
