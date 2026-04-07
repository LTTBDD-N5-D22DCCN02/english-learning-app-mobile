package com.estudy.app.controller;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.estudy.app.R;
import com.estudy.app.api.ApiClient;
import com.estudy.app.api.ApiService;
import com.estudy.app.model.request.AnswerRequest;
import com.estudy.app.model.request.StartSessionRequest;
import com.estudy.app.model.response.ApiResponse;
import com.estudy.app.model.response.SessionResultResponse;
import com.estudy.app.model.response.StartSessionResponse;
import com.estudy.app.model.response.StartSessionResponse.SessionCardResponse;
import com.estudy.app.utils.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WordQuizActivity extends AppCompatActivity {

    public static final String EXTRA_SET_ID   = "set_id";
    public static final String EXTRA_SET_NAME = "set_name";

    private TextView     tvProgress, tvQuestion;
    private Button       btnOption1, btnOption2, btnOption3, btnOption4;
    private ProgressBar  progressBar;

    private ApiService   apiService;
    private String       sessionId;
    private List<SessionCardResponse> cards = new ArrayList<>();
    private int          currentIndex = 0;
    private final List<String> wrongTerms = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_quiz);

        apiService = ApiClient.getInstance(new TokenManager(this)).create(ApiService.class);

        bindViews();
        startSession(getIntent().getStringExtra(EXTRA_SET_ID));
    }

    private void bindViews() {
        tvProgress  = findViewById(R.id.tvProgress);
        tvQuestion  = findViewById(R.id.tvQuestion);
        progressBar = findViewById(R.id.progressBar);
        btnOption1  = findViewById(R.id.btnOption1);
        btnOption2  = findViewById(R.id.btnOption2);
        btnOption3  = findViewById(R.id.btnOption3);
        btnOption4  = findViewById(R.id.btnOption4);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> confirmExit());
        ImageButton btnExit = findViewById(R.id.btnExit);
        if (btnExit != null) btnExit.setOnClickListener(v -> confirmExit());

        setOptionListeners();
    }

    private void setOptionListeners() {
        View.OnClickListener listener = v -> {
            if (!(v instanceof Button)) return;
            Button clicked = (Button) v;
            checkAnswer(clicked);
        };
        btnOption1.setOnClickListener(listener);
        btnOption2.setOnClickListener(listener);
        btnOption3.setOnClickListener(listener);
        btnOption4.setOnClickListener(listener);
    }

    private void startSession(String setId) {
        showLoading(true);
        apiService.startSession(new StartSessionRequest(setId, "word_quiz"))
                .enqueue(new Callback<ApiResponse<StartSessionResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<StartSessionResponse>> call,
                                   Response<ApiResponse<StartSessionResponse>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null
                        && response.body().getResult() != null) {
                    StartSessionResponse data = response.body().getResult();
                    sessionId = data.getSessionId();
                    cards = data.getCards() != null ? data.getCards() : new ArrayList<>();
                    if (cards.isEmpty()) {
                        Toast.makeText(WordQuizActivity.this,
                                "Không có câu hỏi nào!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        showQuestion(0);
                    }
                } else {
                    Toast.makeText(WordQuizActivity.this,
                            "Không thể bắt đầu quiz", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<StartSessionResponse>> call, Throwable t) {
                showLoading(false);
                Toast.makeText(WordQuizActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void showQuestion(int index) {
        if (index >= cards.size()) { endSession(); return; }
        currentIndex = index;

        SessionCardResponse card = cards.get(index);
        int total = cards.size();

        tvProgress.setText((index + 1) + " / " + total);
        progressBar.setProgress((index + 1) * 100 / total);
        tvQuestion.setText(card.getTerm());

        // Build options: correct + 3 distractors (shuffled)
        List<String> options = new ArrayList<>();
        options.add(card.getDefinition() != null ? card.getDefinition() : "");
        List<String> distractors = card.getDistractors();
        if (distractors != null) {
            for (int i = 0; i < Math.min(3, distractors.size()); i++)
                options.add(distractors.get(i));
        }
        // Pad to 4 if needed
        while (options.size() < 4) options.add("—");
        Collections.shuffle(options);

        Button[] btns = {btnOption1, btnOption2, btnOption3, btnOption4};
        for (int i = 0; i < 4; i++) {
            btns[i].setText(i < options.size() ? options.get(i) : "—");
            resetButtonStyle(btns[i]);
            btns[i].setEnabled(true);
        }
    }

    private void checkAnswer(Button clicked) {
        SessionCardResponse card = cards.get(currentIndex);
        String correctDef = card.getDefinition() != null ? card.getDefinition() : "";
        boolean isCorrect = correctDef.equals(clicked.getText().toString());

        // Highlight
        Button[] btns = {btnOption1, btnOption2, btnOption3, btnOption4};
        for (Button btn : btns) btn.setEnabled(false);

        if (isCorrect) {
            clicked.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        } else {
            clicked.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336")));
            wrongTerms.add(card.getTerm());
            // Show correct answer in green
            for (Button btn : btns) {
                if (btn.getText().toString().equals(correctDef)) {
                    btn.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                }
            }
        }

        // Submit answer
        AnswerRequest req = new AnswerRequest(card.getFlashcardId(), sessionId, null, isCorrect);
        apiService.submitAnswer(req).enqueue(new Callback<ApiResponse<Void>>() {
            @Override public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> r) {}
            @Override public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {}
        });

        // Go to next after 800ms
        new Handler().postDelayed(() -> showQuestion(currentIndex + 1), 800);
    }

    private void resetButtonStyle(Button btn) {
        btn.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(Color.parseColor("#005AAE")));
        btn.setTextColor(Color.WHITE);
    }

    private void endSession() {
        if (sessionId == null) { finish(); return; }
        apiService.endSession(sessionId, wrongTerms)
                .enqueue(new Callback<ApiResponse<SessionResultResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<SessionResultResponse>> call,
                                   Response<ApiResponse<SessionResultResponse>> response) {
                goToResult(response.isSuccessful() && response.body() != null
                        ? response.body().getResult() : null);
            }

            @Override
            public void onFailure(Call<ApiResponse<SessionResultResponse>> call, Throwable t) {
                goToResult(null);
            }
        });
    }

    private void goToResult(SessionResultResponse result) {
        Intent intent = new Intent(this, SessionResultActivity.class);
        if (result != null) {
            intent.putExtra(SessionResultActivity.EXTRA_CORRECT,  result.getCorrectCount());
            intent.putExtra(SessionResultActivity.EXTRA_TOTAL,    result.getTotalQuestions());
            intent.putExtra(SessionResultActivity.EXTRA_STREAK,   result.getCurrentStreak());
            intent.putExtra(SessionResultActivity.EXTRA_NEW_RECORD, result.isNewRecord());
            intent.putExtra(SessionResultActivity.EXTRA_DURATION, result.getDurationSeconds());
        } else {
            intent.putExtra(SessionResultActivity.EXTRA_CORRECT, cards.size() - wrongTerms.size());
            intent.putExtra(SessionResultActivity.EXTRA_TOTAL,   cards.size());
        }
        intent.putExtra(SessionResultActivity.EXTRA_MODE, "Word Quiz");
        startActivity(intent);
        finish();
    }

    private void confirmExit() {
        new AlertDialog.Builder(this)
                .setTitle("Thoát khỏi quiz?")
                .setPositiveButton("Thoát", (d, w) -> finish())
                .setNegativeButton("Tiếp tục", null).show();
    }

    private void showLoading(boolean loading) {
        if (progressBar != null)
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
