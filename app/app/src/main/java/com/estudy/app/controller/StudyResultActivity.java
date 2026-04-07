package com.estudy.app.controller;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.estudy.app.R;

public class StudyResultActivity extends AppCompatActivity {

    public static final String EXTRA_CORRECT = "correctCount";
    public static final String EXTRA_WRONG = "wrongCount";
    public static final String EXTRA_SET_ID = "setId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_result);

        int correct = getIntent().getIntExtra(EXTRA_CORRECT, 0);
        int wrong = getIntent().getIntExtra(EXTRA_WRONG, 0);
        String setId = getIntent().getStringExtra(EXTRA_SET_ID);

        int total = correct + wrong;
        int pct = total > 0 ? (int) ((float) correct / total * 100) : 0;

        TextView tvAccuracy = findViewById(R.id.tvAccuracy);
        TextView tvCorrectCount = findViewById(R.id.tvCorrectCount);
        TextView tvWrongCount = findViewById(R.id.tvWrongCount);
        Button btnBack = null;

        tvAccuracy.setText(pct + "%");
        tvCorrectCount.setText("T: " + correct);
        tvWrongCount.setText("F: " + wrong);

        // Toolbar back
        android.widget.ImageButton btnToolbarBack = findViewById(R.id.btnBack);
        if (btnToolbarBack != null) {
            btnToolbarBack.setOnClickListener(v -> finish());
        }

        // Study Again
        Button btnStudyAgain = findViewById(R.id.btnStudyAgain);
        if (btnStudyAgain != null) {
            btnStudyAgain.setOnClickListener(v -> {
                // Go back to study mode selection
                finish();
            });
        }

        // View Statistics (placeholder)
        Button btnViewStats = findViewById(R.id.btnViewStats);
        if (btnViewStats != null) {
            btnViewStats.setOnClickListener(v -> {
                // TODO: open StatisticsActivity when implemented
                android.widget.Toast.makeText(this, "Statistics — coming soon!", android.widget.Toast.LENGTH_SHORT).show();
            });
        }

        // Back to Home
        Button btnBackHome = findViewById(R.id.btnBackHome);
        if (btnBackHome != null) {
            btnBackHome.setOnClickListener(v -> {
                Intent intent = new Intent(this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }
    }
}
