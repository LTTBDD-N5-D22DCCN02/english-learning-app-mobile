package com.estudy.app.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.estudy.app.R;
import com.estudy.app.utils.AccuracyRingView;

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

        // Mode
        TextView tvMode = findViewById(R.id.tvMode);
        if (tvMode != null) tvMode.setText(mode != null ? mode : "");

        // F/T counters
        TextView tvWrong   = findViewById(R.id.tvWrongCount);
        TextView tvCorrect = findViewById(R.id.tvCorrectCount);
        if (tvWrong   != null) tvWrong.setText(String.valueOf(wrong));
        if (tvCorrect != null) tvCorrect.setText(String.valueOf(correct));

        // Duration
        TextView tvDuration = findViewById(R.id.tvDuration);
        if (tvDuration != null) {
            if (durationSeconds > 0) {
                int mins = durationSeconds / 60;
                int secs = durationSeconds % 60;
                tvDuration.setText(String.format("%d:%02d", mins, secs));
            } else {
                tvDuration.setText("—");
            }
        }

        // Streak
        TextView tvStreak = findViewById(R.id.tvStreak);
        if (tvStreak != null) tvStreak.setText("🔥 " + streak);

        // New record badge
        View cardNewRecord = findViewById(R.id.cardNewRecord);
        if (cardNewRecord != null)
            cardNewRecord.setVisibility(isNewRecord ? View.VISIBLE : View.GONE);

        // Motivational message
        TextView tvMotivation = findViewById(R.id.tvMotivation);
        if (tvMotivation != null) {
            if      (accuracy >= 90) tvMotivation.setText("Xuất sắc! Bạn đang tiến bộ rất nhanh! 🌟");
            else if (accuracy >= 70) tvMotivation.setText("Tốt lắm! Hãy tiếp tục luyện tập nhé! 🎉");
            else if (accuracy >= 50) tvMotivation.setText("Cố gắng thêm một chút! Bạn làm được! 👍");
            else                     tvMotivation.setText("Đừng bỏ cuộc! Luyện tập thêm để tiến bộ! 💪");
        }

        // ── Buttons ──

        // Study Again: go back (return to the study screen that launched us)
        Button btnAgain = findViewById(R.id.btnStudyAgain);
        if (btnAgain != null)
            btnAgain.setOnClickListener(v -> finish());

        // View Statistics: open the separate StatisticsActivity
        Button btnStats = findViewById(R.id.btnViewStats);
        if (btnStats != null)
            btnStats.setOnClickListener(v -> {
                Intent intent = new Intent(this, StatisticsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            });

        // Back to Home: navigate to HomeActivity and clear the back-stack
        Button btnDone = findViewById(R.id.btnDone);
        if (btnDone != null)
            btnDone.setOnClickListener(v -> {
                Intent intent = new Intent(this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
    }
}
