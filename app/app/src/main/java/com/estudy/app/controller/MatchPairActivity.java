package com.estudy.app.controller;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
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

import java.util.*;

public class MatchPairActivity extends AppCompatActivity {

    public static final String EXTRA_SET_ID   = "set_id";
    public static final String EXTRA_SET_NAME = "set_name";

    private static final int PAIRS_PER_ROUND = 5;   // 5 cặp mỗi vòng
    private static final int TIMER_PER_ROUND = 90;  // 90s mỗi vòng
    private static final int DISAPPEAR_DELAY = 1000; // 1s trước khi biến mất

    // Views
    private LinearLayout layoutLeftColumn, layoutRightColumn;
    private TextView     tvRoundBadge, tvTimer;
    private Button       btnSkip;

    // API
    private ApiService apiService;
    private String     sessionId;

    // Data
    private List<SessionCardResponse> allCards    = new ArrayList<>();
    private List<SessionCardResponse> remaining   = new ArrayList<>();
    private List<SessionCardResponse> currentBatch = new ArrayList<>();

    // State
    private View   selectedLeftView  = null;
    private View   selectedRightView = null;
    private String selectedLeftId    = null;
    private String selectedRightId   = null;

    private int matchedTotal = 0;
    private int wrongTotal   = 0;
    private int roundNumber  = 0;
    private int totalRounds  = 1;

    // Timer
    private CountDownTimer countDownTimer;
    private int            secondsLeft = TIMER_PER_ROUND;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_pair);

        apiService = ApiClient.getInstance(new TokenManager(this)).create(ApiService.class);
        bindViews();
        startSession(getIntent().getStringExtra(EXTRA_SET_ID));
    }

    @Override
    protected void onDestroy() {
        stopTimer();
        super.onDestroy();
    }

    private void bindViews() {
        layoutLeftColumn  = findViewById(R.id.layoutLeftColumn);
        layoutRightColumn = findViewById(R.id.layoutRightColumn);
        tvRoundBadge      = findViewById(R.id.tvRoundBadge);
        tvTimer           = findViewById(R.id.tvTimer);
        btnSkip           = findViewById(R.id.btnSkip);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> confirmExit());
        ImageButton btnSettings = findViewById(R.id.btnSettings);
        if (btnSettings != null) btnSettings.setOnClickListener(v -> confirmExit());

        if (btnSkip != null) btnSkip.setOnClickListener(v -> skipRound());
    }

    private void startSession(String setId) {
        apiService.startSession(new StartSessionRequest(setId, "match"))
                .enqueue(new Callback<ApiResponse<StartSessionResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<StartSessionResponse>> call,
                                           Response<ApiResponse<StartSessionResponse>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResult() != null) {
                            StartSessionResponse data = response.body().getResult();
                            sessionId = data.getSessionId();
                            allCards  = data.getCards() != null ? data.getCards() : new ArrayList<>();
                            remaining = new ArrayList<>(allCards);

                            if (remaining.isEmpty()) {
                                Toast.makeText(MatchPairActivity.this,
                                        "Không có thẻ nào!", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                // Tính tổng số vòng: ceil(totalCards / PAIRS_PER_ROUND)
                                totalRounds = (int) Math.ceil((double) remaining.size() / PAIRS_PER_ROUND);
                                startNextRound();
                            }
                        } else {
                            Toast.makeText(MatchPairActivity.this,
                                    "Không thể bắt đầu game", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                    @Override
                    public void onFailure(Call<ApiResponse<StartSessionResponse>> call, Throwable t) {
                        Toast.makeText(MatchPairActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    // ── Round management ─────────────────────────────────────

    private void startNextRound() {
        roundNumber++;
        if (remaining.isEmpty()) { endSession(); return; }

        // Lấy batch tiếp theo (tối đa PAIRS_PER_ROUND)
        int batchSize = Math.min(PAIRS_PER_ROUND, remaining.size());
        currentBatch = new ArrayList<>(remaining.subList(0, batchSize));

        // Cập nhật badge vòng
        updateRoundBadge();

        // Build board
        buildBoard();

        // Start countdown timer
        startTimer();
    }

    private void updateRoundBadge() {
        if (tvRoundBadge != null) {
            tvRoundBadge.setText(roundNumber + "/" + totalRounds);
        }
    }

    private void skipRound() {
        stopTimer();
        // Đánh dấu các từ trong batch hiện tại là sai
        for (SessionCardResponse card : currentBatch) {
            wrongTotal++;
            submitAnswer(card.getFlashcardId(), false);
        }
        remaining.removeAll(currentBatch);
        if (remaining.isEmpty()) {
            endSession();
        } else {
            startNextRound();
        }
    }

    // ── Timer ────────────────────────────────────────────────

    private void startTimer() {
        stopTimer();
        secondsLeft = TIMER_PER_ROUND;
        updateTimerUI();

        countDownTimer = new CountDownTimer(TIMER_PER_ROUND * 1000L, 1000) {
            @Override public void onTick(long ms) {
                secondsLeft = (int)(ms / 1000);
                updateTimerUI();
                // Đổi màu đỏ khi còn ≤ 10s
                if (tvTimer != null) {
                    tvTimer.setAlpha(secondsLeft <= 10 ? 0.7f : 1f);
                }
            }
            @Override public void onFinish() {
                secondsLeft = 0;
                updateTimerUI();
                // Hết giờ → skip round
                Toast.makeText(MatchPairActivity.this, "Hết giờ!", Toast.LENGTH_SHORT).show();
                skipRound();
            }
        }.start();
    }

    private void stopTimer() {
        if (countDownTimer != null) { countDownTimer.cancel(); countDownTimer = null; }
    }

    private void updateTimerUI() {
        if (tvTimer != null) tvTimer.setText(secondsLeft + "s");
    }

    // ── Board building ────────────────────────────────────────

    private void buildBoard() {
        layoutLeftColumn.removeAllViews();
        layoutRightColumn.removeAllViews();
        selectedLeftView = null; selectedRightView = null;
        selectedLeftId = null;   selectedRightId   = null;

        // Left: terms theo thứ tự
        List<SessionCardResponse> leftOrder = new ArrayList<>(currentBatch);
        // Right: definitions trộn ngẫu nhiên
        List<SessionCardResponse> rightOrder = new ArrayList<>(currentBatch);
        Collections.shuffle(rightOrder);

        for (SessionCardResponse card : leftOrder) {
            View cell = makePairCell(card.getTerm(), "left");
            cell.setTag(card.getFlashcardId());
            cell.setOnClickListener(v -> onCellClick(cell, card.getFlashcardId(), true));
            layoutLeftColumn.addView(cell);
            layoutLeftColumn.addView(spacer());
        }

        for (SessionCardResponse card : rightOrder) {
            String def = card.getDefinition() != null ? card.getDefinition() : "";
            View cell = makePairCell(def, "right");
            cell.setTag(card.getFlashcardId());
            cell.setOnClickListener(v -> onCellClick(cell, card.getFlashcardId(), false));
            layoutRightColumn.addView(cell);
            layoutRightColumn.addView(spacer());
        }
    }

    // ── Click handling ────────────────────────────────────────

    private void onCellClick(View cell, String cardId, boolean isLeft) {
        if (!cell.isEnabled()) return;

        if (isLeft) {
            // Deselect previous left
            if (selectedLeftView != null) resetCellStyle(selectedLeftView);
            selectedLeftView = cell;
            selectedLeftId   = cardId;
            highlightSelected(cell);
        } else {
            // Deselect previous right
            if (selectedRightView != null) resetCellStyle(selectedRightView);
            selectedRightView = cell;
            selectedRightId   = cardId;
            highlightSelected(cell);
        }

        // Khi đã chọn cả 2 → check match
        if (selectedLeftView != null && selectedRightView != null) {
            checkMatch();
        }
    }

    private void checkMatch() {
        View leftCell  = selectedLeftView;
        View rightCell = selectedRightView;
        String leftId  = selectedLeftId;
        String rightId = selectedRightId;

        selectedLeftView = null; selectedRightView = null;
        selectedLeftId = null;   selectedRightId   = null;

        // Disable tất cả trong khi xử lý
        setAllEnabled(false);

        if (leftId.equals(rightId)) {
            // ✅ Đúng → highlight green, sau DISAPPEAR_DELAY thì biến mất
            setCellCorrect(leftCell);
            setCellCorrect(rightCell);
            matchedTotal++;
            submitAnswer(leftId, true);

            new Handler().postDelayed(() -> {
                leftCell.setVisibility(View.INVISIBLE);
                rightCell.setVisibility(View.INVISIBLE);
                // Xóa khỏi remaining
                remaining.removeIf(c -> c.getFlashcardId().equals(leftId));
                currentBatch.removeIf(c -> c.getFlashcardId().equals(leftId));

                setAllEnabled(true);

                // Hết cặp trong batch → vòng mới
                if (currentBatch.isEmpty()) {
                    stopTimer();
                    if (remaining.isEmpty()) {
                        new Handler().postDelayed(this::endSession, 300);
                    } else {
                        new Handler().postDelayed(this::startNextRound, 500);
                    }
                }
            }, DISAPPEAR_DELAY);
        } else {
            // ❌ Sai → highlight red, reset sau 600ms
            setCellWrong(leftCell);
            setCellWrong(rightCell);
            wrongTotal++;
            submitAnswer(leftId, false);

            new Handler().postDelayed(() -> {
                resetCellStyle(leftCell);
                resetCellStyle(rightCell);
                setAllEnabled(true);
            }, 600);
        }
    }

    // ── Cell styling ──────────────────────────────────────────

    private View makePairCell(String text, String side) {
        TextView tv = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(64));
        tv.setLayoutParams(lp);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#005AAE"));
        tv.setTextSize(13f);
        tv.setMaxLines(3);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6));
        tv.setBackground(makeStateDrawable());
        tv.setClickable(true);
        tv.setFocusable(true);
        return tv;
    }

    private android.graphics.drawable.Drawable makeStateDrawable() {
        // Default: nền DFE7EF bo góc 12dp
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(Color.parseColor("#DFE7EF"));
        d.setCornerRadius(dpToPx(12));
        return d;
    }

    private void highlightSelected(View cell) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(Color.parseColor("#DFE7EF"));
        d.setCornerRadius(dpToPx(12));
        d.setStroke(dpToPx(2), Color.parseColor("#005AAE"));
        cell.setBackground(d);
    }

    private void resetCellStyle(View cell) {
        cell.setBackground(makeStateDrawable());
    }

    private void setCellCorrect(View cell) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(Color.parseColor("#DBFCE7"));
        d.setCornerRadius(dpToPx(12));
        d.setStroke(dpToPx(2), Color.parseColor("#4FB968"));
        cell.setBackground(d);
    }

    private void setCellWrong(View cell) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(Color.parseColor("#FFE2E2"));
        d.setCornerRadius(dpToPx(12));
        d.setStroke(dpToPx(2), Color.parseColor("#FB2C36"));
        cell.setBackground(d);
    }

    private void setAllEnabled(boolean enabled) {
        setColumnEnabled(layoutLeftColumn, enabled);
        setColumnEnabled(layoutRightColumn, enabled);
    }

    private void setColumnEnabled(LinearLayout col, boolean enabled) {
        for (int i = 0; i < col.getChildCount(); i++) {
            View v = col.getChildAt(i);
            if (v.getVisibility() == View.VISIBLE) v.setEnabled(enabled);
        }
    }

    // ── Submit answer ────────────────────────────────────────

    private void submitAnswer(String flashcardId, boolean correct) {
        if (sessionId == null) return;
        AnswerRequest req = new AnswerRequest(flashcardId, sessionId, null, correct);
        apiService.submitAnswer(req).enqueue(new Callback<ApiResponse<Void>>() {
            @Override public void onResponse(Call<ApiResponse<Void>> c, Response<ApiResponse<Void>> r) {}
            @Override public void onFailure(Call<ApiResponse<Void>> c, Throwable t) {}
        });
    }

    // ── End session ───────────────────────────────────────────

    private void endSession() {
        stopTimer();
        if (sessionId == null) { finish(); return; }
        List<String> wrongTerms = new ArrayList<>();
        for (int i = 0; i < wrongTotal; i++) wrongTerms.add("error");

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
            intent.putExtra(SessionResultActivity.EXTRA_CORRECT,    result.getCorrectCount());
            intent.putExtra(SessionResultActivity.EXTRA_TOTAL,      result.getTotalQuestions());
            intent.putExtra(SessionResultActivity.EXTRA_STREAK,     result.getCurrentStreak());
            intent.putExtra(SessionResultActivity.EXTRA_NEW_RECORD, result.isNewRecord());
            intent.putExtra(SessionResultActivity.EXTRA_DURATION,   result.getDurationSeconds());
        } else {
            intent.putExtra(SessionResultActivity.EXTRA_CORRECT, matchedTotal);
            intent.putExtra(SessionResultActivity.EXTRA_TOTAL,   allCards.size());
        }
        intent.putExtra(SessionResultActivity.EXTRA_MODE, "Match the Pairs");
        startActivity(intent);
        finish();
    }

    // ── Helpers ───────────────────────────────────────────────

    private View spacer() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(10)));
        return v;
    }

    private int dpToPx(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }

    private void confirmExit() {
        stopTimer();
        new AlertDialog.Builder(this)
                .setTitle("Thoát khỏi game?")
                .setMessage("Tiến độ hiện tại sẽ được lưu lại.")
                .setPositiveButton("Thoát", (d, w) -> {
                    if (sessionId != null) {
                        List<String> wt = new ArrayList<>();
                        for (int i = 0; i < wrongTotal; i++) wt.add("error");
                        apiService.endSession(sessionId, wt).enqueue(
                                new Callback<ApiResponse<SessionResultResponse>>() {
                                    @Override public void onResponse(Call<ApiResponse<SessionResultResponse>> c, Response<ApiResponse<SessionResultResponse>> r) { finish(); }
                                    @Override public void onFailure(Call<ApiResponse<SessionResultResponse>> c, Throwable t) { finish(); }
                                });
                    } else finish();
                })
                .setNegativeButton("Tiếp tục", (d, w) -> startTimer())
                .show();
    }
}