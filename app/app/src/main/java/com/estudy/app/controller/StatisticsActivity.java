package com.estudy.app.controller;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.estudy.app.R;
import com.estudy.app.api.ApiClient;
import com.estudy.app.api.ApiService;
import com.estudy.app.model.response.*;
import com.estudy.app.utils.AccuracyRingView;
import com.estudy.app.utils.BottomNavHelper;
import com.estudy.app.utils.NavHelper;
import com.estudy.app.utils.StudyBarChartView;
import com.estudy.app.utils.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.*;

public class StatisticsActivity extends AppCompatActivity {

    // Vocab Progress
    private TextView tvWordsLearned, tvWordsMastered;

    // Accuracy
    private AccuracyRingView accuracyRing;
    private TextView tvWrongLabel, tvTotalLabel;

    // Accuracy trend
    private StudyBarChartView accuracyTrendChart;
    private TextView tvAvgAccuracy, tvAccuracyTrendIcon, tvAccuracyTrend;
    private TextView btnWeekly, btnMonthly;

    // Streak
    private TextView tvCurrentStreak, tvLongestStreak;
    private LinearLayout badgeNewRecord;

    // Activity chart
    private StudyBarChartView barChart;
    private TextView tvActivitySum, tvActivityAvg, tvActivityPeriod;

    // Set progress
    private RecyclerView rvSetProgress;

    // Quote
    private TextView tvQuote;

    // Misc
    private LinearLayout layoutStatEmpty;
    private ProgressBar progressBarStat;
    private ApiService apiService;

    // ── Quotes pool ──────────────────────────────────────────────
    private static final String[] QUOTES = {
            "\"Small steps every day lead to big success.\"",
            "\"The secret of getting ahead is getting started.\"",
            "\"Every word you learn is a new door to the world.\"",
            "\"Fluency is not a destination, it's a daily practice.\"",
            "\"You don't have to be great to start, but you have to start to be great.\"",
            "\"Learning a language is like building a house — one brick at a time.\"",
            "\"Consistency beats intensity every single time.\"",
            "\"The best time to learn was yesterday. The next best time is now.\"",
            "\"Languages are the road map of a culture.\"",
            "\"With languages, you are at home anywhere.\"",
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        BottomNavHelper.setup(this, R.id.btnNavStats);

        TokenManager tm = new TokenManager(this);
        apiService = ApiClient.getInstance(tm).create(ApiService.class);

        setupToolbar();
        bindViews();
        showRandomQuote();
        loadStatistics("weekly");
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStatistics("weekly");
    }

    private void setupToolbar() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> NavHelper.back(this));
    }

    private void bindViews() {
        tvWordsLearned      = findViewById(R.id.tvWordsLearned);
        tvWordsMastered     = findViewById(R.id.tvWordsMastered);
        accuracyRing        = findViewById(R.id.accuracyRing);
        tvWrongLabel        = findViewById(R.id.tvWrongLabel);
        tvTotalLabel        = findViewById(R.id.tvTotalLabel);
        accuracyTrendChart  = findViewById(R.id.accuracyTrendChart);
        tvAvgAccuracy       = findViewById(R.id.tvAvgAccuracy);
        tvAccuracyTrendIcon = findViewById(R.id.tvAccuracyTrendIcon);
        tvAccuracyTrend     = findViewById(R.id.tvAccuracyTrend);
        btnWeekly           = findViewById(R.id.btnWeekly);
        btnMonthly          = findViewById(R.id.btnMonthly);
        tvCurrentStreak     = findViewById(R.id.tvCurrentStreak);
        tvLongestStreak     = findViewById(R.id.tvLongestStreak);
        badgeNewRecord      = findViewById(R.id.badgeNewRecord);
        barChart            = findViewById(R.id.barChart);
        tvActivitySum       = findViewById(R.id.tvActivitySum);
        tvActivityAvg       = findViewById(R.id.tvActivityAvg);
        tvActivityPeriod    = findViewById(R.id.tvActivityPeriod);
        rvSetProgress       = findViewById(R.id.rvSetProgress);
        layoutStatEmpty     = findViewById(R.id.layoutStatEmpty);
        progressBarStat     = findViewById(R.id.progressBarStat);
        tvQuote             = findViewById(R.id.tvQuote);

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

        // Heart / quote refresh on click
        View ivHeart = findViewById(R.id.ivQuoteHeart);
        if (ivHeart != null) ivHeart.setOnClickListener(v -> showRandomQuote());
    }

    // ── Random quote ─────────────────────────────────────────────
    private void showRandomQuote() {
        if (tvQuote == null) return;
        int idx = (int)(Math.random() * QUOTES.length);
        tvQuote.setText(QUOTES[idx]);
    }

    // ── Load all ─────────────────────────────────────────────────
    private void loadStatistics(String period) {
        loadStatSummary();
        loadActivityChart(period);
        loadSetProgress();
        setPeriodSelected(period.equals("weekly"));
    }

    // ── UC-STAT-01,02,03 ─────────────────────────────────────────
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

    private void bindStatSummary(StatSummaryResponse s) {
        // UC-STAT-01
        if (tvWordsLearned  != null) tvWordsLearned.setText(String.valueOf(s.getWordsLearned()));
        if (tvWordsMastered != null) tvWordsMastered.setText(String.valueOf(s.getWordsMastered()));

        // UC-STAT-02
        long wrong = s.getWrongAnswers();
        long total = s.getTotalAnswers();
        double pct = s.getAccuracyPercent();
        if (accuracyRing != null)
            accuracyRing.setResult((int)(total - wrong), (int) total);
        if (tvWrongLabel  != null) tvWrongLabel.setText("F: " + wrong);
        if (tvTotalLabel  != null) tvTotalLabel.setText("T: " + total);

        // UC-STAT-03
        if (tvCurrentStreak != null)
            tvCurrentStreak.setText(s.getCurrentStreak() + " days");
        if (tvLongestStreak != null)
            tvLongestStreak.setText(s.getLongestStreak() + " days");
        if (badgeNewRecord  != null)
            badgeNewRecord.setVisibility(s.isNewRecord() ? View.VISIBLE : View.GONE);

        boolean hasData = total > 0 || s.getWordsLearned() > 0;
        if (layoutStatEmpty != null)
            layoutStatEmpty.setVisibility(hasData ? View.GONE : View.VISIBLE);
    }

    // ── UC-STAT-04,05 ─────────────────────────────────────────────
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
                            if (accuracyTrendChart != null) accuracyTrendChart.setData(data);
                            bindActivitySummary(data, period);
                            computeAndShowTrend(data);
                        }
                    }
                    @Override
                    public void onFailure(Call<ApiResponse<List<DayActivityResponse>>> call, Throwable t) {}
                });
    }

    private void bindActivitySummary(List<DayActivityResponse> data, String period) {
        int sum = 0;
        for (DayActivityResponse d : data) sum += d.getWordCount();
        int days = period.equals("monthly") ? 30 : 7;
        int avg  = days > 0 ? sum / days : 0;
        if (tvActivitySum  != null) tvActivitySum.setText("Sum: " + sum);
        if (tvActivityAvg  != null) tvActivityAvg.setText("Avg: " + avg + "/day");
        if (tvActivityPeriod != null) tvActivityPeriod.setText(period.equals("monthly") ? "Monthly" : "Weekly");
    }

    private void computeAndShowTrend(List<DayActivityResponse> data) {
        if (data == null || data.isEmpty()) return;
        List<Double> acc = new ArrayList<>();
        for (DayActivityResponse d : data)
            if (d.getWordCount() > 0) acc.add(d.getAccuracyPercent());
        if (acc.isEmpty()) return;

        double total = 0;
        for (double a : acc) total += a;
        double avg = total / acc.size();
        if (tvAvgAccuracy != null) tvAvgAccuracy.setText(String.format("%.0f%%", avg));

        if (acc.size() < 2) {
            if (tvAccuracyTrendIcon != null) tvAccuracyTrendIcon.setText("➡️");
            if (tvAccuracyTrend     != null) tvAccuracyTrend.setText("Not enough data");
            return;
        }
        int mid = acc.size() / 2;
        double s1 = 0, s2 = 0;
        for (int i = 0; i < mid; i++) s1 += acc.get(i);
        for (int i = mid; i < acc.size(); i++) s2 += acc.get(i);
        double delta = s2 / (acc.size() - mid) - s1 / mid;
        String icon, label;
        if (delta > 3)       { icon = "📈"; label = String.format("+%.0f%% vs before", delta); }
        else if (delta < -3) { icon = "📉"; label = String.format("%.0f%% vs before", delta); }
        else                 { icon = "➡️"; label = "Stable"; }
        if (tvAccuracyTrendIcon != null) tvAccuracyTrendIcon.setText(icon);
        if (tvAccuracyTrend     != null) tvAccuracyTrend.setText(label);
    }

    // ── UC-STAT-06 ────────────────────────────────────────────────
    private void loadSetProgress() {
        apiService.getSetProgress()
                .enqueue(new Callback<ApiResponse<List<SetProgressResponse>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<SetProgressResponse>>> call,
                                           Response<ApiResponse<List<SetProgressResponse>>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResult() != null && rvSetProgress != null) {
                            rvSetProgress.setAdapter(
                                    new SetProgressAdapter(response.body().getResult()));
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

    // ── SetProgressAdapter (inline) ───────────────────────────────
    private static class SetProgressAdapter
            extends RecyclerView.Adapter<SetProgressAdapter.VH> {

        private final List<SetProgressResponse> items;
        SetProgressAdapter(List<SetProgressResponse> items) { this.items = items; }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_set_progress, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            SetProgressResponse item = items.get(pos);
            h.tvName.setText(item.getSetName());
            h.tvPct.setText(String.format("%.0f%%", item.getPercentage()));
            h.tvCount.setText(item.getRememberedCount() + "/" + item.getTotalWords() + " từ");
            h.progressBar.setProgress((int) item.getPercentage());
        }

        @Override public int getItemCount() { return Math.min(items.size(), 5); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvPct, tvCount;
            ProgressBar progressBar;
            VH(View v) {
                super(v);
                tvName      = v.findViewById(R.id.tvSetName);
//                tvPct       = v.findViewById(R.id.tvSetPct);
                tvCount     = v.findViewById(R.id.tvSetCount);
//                progressBar = v.findViewById(R.id.progressSetBar);
            }
        }
    }
}