package com.estudy.app.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.estudy.app.R;
import com.estudy.app.utils.AccuracyRingView;

import java.util.Locale;

public class SessionResultActivity extends AppCompatActivity {

    public static final String EXTRA_CORRECT    = "correct";
    public static final String EXTRA_TOTAL      = "total";
    public static final String EXTRA_STREAK     = "streak";
    public static final String EXTRA_NEW_RECORD = "new_record";
    public static final String EXTRA_DURATION   = "duration";
    public static final String EXTRA_MODE       = "mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_result);

        int     correct  = getIntent().getIntExtra(EXTRA_CORRECT, 0);
        int     total    = getIntent().getIntExtra(EXTRA_TOTAL, 0);
        int     streak   = getIntent().getIntExtra(EXTRA_STREAK, 0);
        boolean isNew    = getIntent().getBooleanExtra(EXTRA_NEW_RECORD, false);
        int     duration = getIntent().getIntExtra(EXTRA_DURATION, 0);
        String  mode     = getIntent().getStringExtra(EXTRA_MODE);

        bindViews(correct, total, streak, isNew, duration, mode);
    }

    private void bindViews(int correct, int total, int streak,
                           boolean isNewRecord, int durationSeconds, String mode) {

        int wrong = total - correct;
        double accuracy = total > 0 ? correct * 100.0 / total : 0.0;

        // Accuracy ring
        AccuracyRingView ring = findViewById(R.id.accuracyRing);
        if (ring != null) ring.setResult(correct, total);

        // Mode label — tvMode có thể không có trong layout mới, guard bằng null check
        TextView tvMode = findViewById(R.id.tvMode);
        if (tvMode != null) tvMode.setText(mode != null ? mode : "");

        // F / T counters
        TextView tvWrong   = findViewById(R.id.tvWrongCount);
        TextView tvCorrect = findViewById(R.id.tvCorrectCount);
        if (tvWrong   != null) tvWrong.setText(String.valueOf(wrong));
        if (tvCorrect != null) tvCorrect.setText(String.valueOf(correct));

        // Duration — dùng Locale.getDefault() để tránh lint warning
        TextView tvDuration = findViewById(R.id.tvDuration);
        if (tvDuration != null) {
            if (durationSeconds > 0) {
                int mins = durationSeconds / 60;
                int secs = durationSeconds % 60;
                tvDuration.setText(String.format(Locale.getDefault(), "%d:%02d", mins, secs));
            } else {
                tvDuration.setText(R.string.dash);
            }
        }

        // Streak
        TextView tvStreak = findViewById(R.id.tvStreak);
        if (tvStreak != null) tvStreak.setText(String.valueOf(streak));

        // New record badge
        View cardNewRecord = findViewById(R.id.cardNewRecord);
        if (cardNewRecord != null)
            cardNewRecord.setVisibility(isNewRecord ? View.VISIBLE : View.GONE);

        // Motivational message — dùng string resources, không hardcode
        TextView tvMotivation = findViewById(R.id.tvMotivation);
        if (tvMotivation != null) {
            if      (accuracy >= 90) tvMotivation.setText(R.string.motivation_excellent);
            else if (accuracy >= 70) tvMotivation.setText(R.string.motivation_good);
            else if (accuracy >= 50) tvMotivation.setText(R.string.motivation_ok);
            else                     tvMotivation.setText(R.string.motivation_keep_going);
        }

        // Back arrow
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Study Again
        View btnAgain = findViewById(R.id.btnStudyAgain);
        if (btnAgain != null) btnAgain.setOnClickListener(v -> finish());

        // View Statistics
        View btnStats = findViewById(R.id.btnViewStats);
        if (btnStats != null)
            btnStats.setOnClickListener(v -> {
                Intent intent = new Intent(this, StatisticsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            });

        // Back to Home
        View btnDone = findViewById(R.id.btnDone);
        if (btnDone != null)
            btnDone.setOnClickListener(v -> {
                Intent intent = new Intent(this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
    }
}