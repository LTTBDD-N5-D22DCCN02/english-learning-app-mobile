package com.estudy.app.controller;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.estudy.app.R;
import com.estudy.app.api.ApiClient;
import com.estudy.app.api.ApiService;
import com.estudy.app.model.request.AnswerRequest;
import com.estudy.app.model.response.ApiResponse;
import com.estudy.app.model.response.FlashCardResponse;
import com.estudy.app.model.response.FlashCardSetDetailResponse;
import com.estudy.app.utils.TokenManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MatchActivity extends AppCompatActivity {

    public static final String EXTRA_SET_ID = "setId";
    public static final String EXTRA_SET_NAME = "setName";
    private static final int PAIRS_PER_ROUND = 5;
    private static final int TIMER_SECONDS = 90;

    private TextView tvProgress, tvTimer;
    private LinearLayout layoutLeftColumn, layoutRightColumn;
    private Button btnSkip;

    private ApiService apiService;
    private String setId;

    private List<FlashCardResponse> allCards = new ArrayList<>();
    private List<FlashCardResponse> currentPairs = new ArrayList<>();
    private List<String> leftItems = new ArrayList<>();  // terms
    private List<String> rightItems = new ArrayList<>(); // definitions
    private List<TextView> leftViews = new ArrayList<>();
    private List<TextView> rightViews = new ArrayList<>();

    private int selectedLeft = -1;
    private int selectedRight = -1;
    private int matchedCount = 0;
    private int roundStartIndex = 0;
    private int totalMatched = 0;
    private int totalWrong = 0;

    private CountDownTimer countDownTimer;
    private int timeLeft = TIMER_SECONDS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match);

        setId = getIntent().getStringExtra(EXTRA_SET_ID);

        TokenManager tm = new TokenManager(this);
        apiService = ApiClient.getInstance(tm).create(ApiService.class);

        tvProgress = findViewById(R.id.tvProgress);
        tvTimer = findViewById(R.id.tvTimer);
        layoutLeftColumn = findViewById(R.id.layoutLeftColumn);
        layoutRightColumn = findViewById(R.id.layoutRightColumn);
        btnSkip = findViewById(R.id.btnSkip);

        btnSkip.setOnClickListener(v -> {
            // Skip current round
            openResult();
        });

        loadCards();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }

    private void loadCards() {
        apiService.getFlashCardSetDetail(setId).enqueue(new Callback<ApiResponse<FlashCardSetDetailResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<FlashCardSetDetailResponse>> call,
                                   Response<ApiResponse<FlashCardSetDetailResponse>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getResult() != null) {
                    List<FlashCardResponse> all = response.body().getResult().getFlashCards();
                    if (all != null && !all.isEmpty()) {
                        allCards = new ArrayList<>(all);
                        Collections.shuffle(allCards);
                        startRound(0);
                        startTimer();
                    } else {
                        Toast.makeText(MatchActivity.this, "Không có từ", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    Toast.makeText(MatchActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<FlashCardSetDetailResponse>> call, Throwable t) {
                Toast.makeText(MatchActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void startRound(int startIdx) {
        roundStartIndex = startIdx;
        selectedLeft = -1;
        selectedRight = -1;
        matchedCount = 0;

        int end = Math.min(startIdx + PAIRS_PER_ROUND, allCards.size());
        currentPairs = new ArrayList<>(allCards.subList(startIdx, end));

        tvProgress.setText((startIdx + 1) + "/" + allCards.size());

        // Build left (terms) and right (definitions) lists, shuffle independently
        leftItems = new ArrayList<>();
        rightItems = new ArrayList<>();
        for (FlashCardResponse c : currentPairs) {
            leftItems.add(c.getTerm() != null ? c.getTerm() : "");
            String def = c.getDefinition() != null ? c.getDefinition() : "";
            // Truncate long definitions to keep UI clean
            if (def.length() > 30) def = def.substring(0, 27) + "...";
            rightItems.add(def);
        }
        Collections.shuffle(rightItems);

        buildColumns();
    }

    private void buildColumns() {
        layoutLeftColumn.removeAllViews();
        layoutRightColumn.removeAllViews();
        leftViews.clear();
        rightViews.clear();

        for (int i = 0; i < leftItems.size(); i++) {
            final int idx = i;
            TextView tvLeft = makeCard(leftItems.get(i));
            tvLeft.setOnClickListener(v -> onLeftClick(idx));
            layoutLeftColumn.addView(tvLeft);
            leftViews.add(tvLeft);

            TextView tvRight = makeCard(rightItems.get(i));
            tvRight.setOnClickListener(v -> onRightClick(idx));
            layoutRightColumn.addView(tvRight);
            rightViews.add(tvRight);
        }
    }

    private TextView makeCard(String text) {
        TextView tv = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        tv.setLayoutParams(lp);
        tv.setText(text);
        tv.setTextSize(13f);
        tv.setTextColor(Color.parseColor("#1e293b"));
        tv.setGravity(Gravity.CENTER);
        tv.setMinHeight(dp(52));
        tv.setPadding(dp(8), dp(10), dp(8), dp(10));
        tv.setBackground(getDrawable(R.drawable.bg_match_card));
        return tv;
    }

    private void onLeftClick(int idx) {
        if (leftViews.get(idx).getVisibility() != android.view.View.VISIBLE) return;
        // Deselect previous
        if (selectedLeft >= 0 && selectedLeft < leftViews.size()) {
            leftViews.get(selectedLeft).setBackground(getDrawable(R.drawable.bg_match_card));
        }
        selectedLeft = idx;
        leftViews.get(idx).setBackground(getDrawable(R.drawable.bg_match_selected));
        tryMatch();
    }

    private void onRightClick(int idx) {
        if (rightViews.get(idx).getVisibility() != android.view.View.VISIBLE) return;
        // Deselect previous
        if (selectedRight >= 0 && selectedRight < rightViews.size()) {
            rightViews.get(selectedRight).setBackground(getDrawable(R.drawable.bg_match_card));
        }
        selectedRight = idx;
        rightViews.get(idx).setBackground(getDrawable(R.drawable.bg_match_selected));
        tryMatch();
    }

    private void tryMatch() {
        if (selectedLeft < 0 || selectedRight < 0) return;

        // Find if left[selectedLeft] term matches right[selectedRight] definition
        String selectedTerm = leftItems.get(selectedLeft);
        String selectedDef = rightItems.get(selectedRight);

        // Find the card with this term
        boolean correct = false;
        for (FlashCardResponse card : currentPairs) {
            if (card.getTerm() != null && card.getTerm().equals(selectedTerm)) {
                String cardDef = card.getDefinition() != null ? card.getDefinition() : "";
                if (cardDef.length() > 30) cardDef = cardDef.substring(0, 27) + "...";
                if (cardDef.equals(selectedDef)) {
                    correct = true;
                }
                break;
            }
        }

        final int l = selectedLeft;
        final int r = selectedRight;

        if (correct) {
            totalMatched++;
            matchedCount++;
            leftViews.get(l).setBackground(getDrawable(R.drawable.bg_correct_feedback));
            rightViews.get(r).setBackground(getDrawable(R.drawable.bg_correct_feedback));

            // Submit answer
            submitMatchAnswer(selectedTerm, true);

            // Hide after short delay
            layoutLeftColumn.postDelayed(() -> {
                leftViews.get(l).setVisibility(android.view.View.INVISIBLE);
                rightViews.get(r).setVisibility(android.view.View.INVISIBLE);
                checkRoundComplete();
            }, 500);
        } else {
            totalWrong++;
            leftViews.get(l).setBackground(getDrawable(R.drawable.bg_match_wrong));
            rightViews.get(r).setBackground(getDrawable(R.drawable.bg_match_wrong));

            // Submit wrong answer
            submitMatchAnswer(selectedTerm, false);

            // Flash red then reset
            layoutLeftColumn.postDelayed(() -> {
                if (l < leftViews.size()) leftViews.get(l).setBackground(getDrawable(R.drawable.bg_match_card));
                if (r < rightViews.size()) rightViews.get(r).setBackground(getDrawable(R.drawable.bg_match_card));
            }, 600);
        }

        selectedLeft = -1;
        selectedRight = -1;
    }

    private void checkRoundComplete() {
        if (matchedCount >= currentPairs.size()) {
            int nextStart = roundStartIndex + PAIRS_PER_ROUND;
            if (nextStart >= allCards.size()) {
                openResult();
            } else {
                startRound(nextStart);
            }
        }
    }

    private void submitMatchAnswer(String term, boolean correct) {
        // Find flashcard ID by term
        for (FlashCardResponse card : currentPairs) {
            if (term.equals(card.getTerm())) {
                AnswerRequest req = new AnswerRequest(card.getId(), setId, "match", correct);
                apiService.submitAnswer(req).enqueue(new Callback<ApiResponse<Void>>() {
                    @Override public void onResponse(Call<ApiResponse<Void>> c, Response<ApiResponse<Void>> r) {}
                    @Override public void onFailure(Call<ApiResponse<Void>> c, Throwable t) {}
                });
                break;
            }
        }
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(TIMER_SECONDS * 1000L, 1000) {
            @Override
            public void onTick(long ms) {
                timeLeft = (int) (ms / 1000);
                tvTimer.setText(timeLeft + "s");
                if (timeLeft <= 10) {
                    tvTimer.setTextColor(Color.parseColor("#EF4444"));
                }
            }

            @Override
            public void onFinish() {
                timeLeft = 0;
                tvTimer.setText("0s");
                openResult();
            }
        }.start();
    }

    private void openResult() {
        if (countDownTimer != null) countDownTimer.cancel();
        Intent intent = new Intent(this, StudyResultActivity.class);
        intent.putExtra(StudyResultActivity.EXTRA_CORRECT, totalMatched);
        intent.putExtra(StudyResultActivity.EXTRA_WRONG, totalWrong);
        intent.putExtra(StudyResultActivity.EXTRA_SET_ID, setId);
        startActivity(intent);
        finish();
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
