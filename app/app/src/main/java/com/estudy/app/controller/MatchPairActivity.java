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

/**
 * UC-STUDY-04: Match the Pairs
 * Hiển thị cột từ (trái) và cột định nghĩa (phải). User chọn 1 từ + 1 định nghĩa để ghép.
 */
public class MatchPairActivity extends AppCompatActivity {

    public static final String EXTRA_SET_ID   = "set_id";
    public static final String EXTRA_SET_NAME = "set_name";

    private static final int MAX_PAIRS = 6; // Tối đa 6 cặp mỗi vòng

    private LinearLayout layoutLeftColumn, layoutRightColumn;
    private TextView     tvProgress;

    private ApiService apiService;
    private String     sessionId;
    private List<SessionCardResponse> cards = new ArrayList<>();

    // Match state
    private Button    selectedLeft  = null;
    private Button    selectedRight = null;
    private String    selectedLeftId  = null;
    private String    selectedRightTerm = null;
    private int       matchedCount  = 0;
    private int       wrongCount    = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_pair);

        apiService = ApiClient.getInstance(new TokenManager(this)).create(ApiService.class);
        bindViews();
        startSession(getIntent().getStringExtra(EXTRA_SET_ID));
    }

    private void bindViews() {
        layoutLeftColumn  = findViewById(R.id.layoutLeftColumn);
        layoutRightColumn = findViewById(R.id.layoutRightColumn);
        tvProgress        = findViewById(R.id.tvProgress);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> confirmExit());
        ImageButton btnExit = findViewById(R.id.btnExit);
        if (btnExit != null) btnExit.setOnClickListener(v -> confirmExit());
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
                    cards = data.getCards() != null ? data.getCards() : new ArrayList<>();
                    if (cards.isEmpty()) {
                        Toast.makeText(MatchPairActivity.this,
                                "Không có thẻ nào!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        buildMatchBoard();
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

    private void buildMatchBoard() {
        layoutLeftColumn.removeAllViews();
        layoutRightColumn.removeAllViews();
        selectedLeft = null; selectedRight = null;
        selectedLeftId = null; selectedRightTerm = null;

        // Lấy tối đa MAX_PAIRS thẻ chưa matched
        List<SessionCardResponse> batch = cards.subList(
                0, Math.min(MAX_PAIRS, cards.size()));

        // Left: terms (theo thứ tự)
        List<SessionCardResponse> leftOrder = new ArrayList<>(batch);
        // Right: definitions (trộn ngẫu nhiên)
        List<SessionCardResponse> rightOrder = new ArrayList<>(batch);
        Collections.shuffle(rightOrder);

        for (SessionCardResponse card : leftOrder) {
            Button btn = makeButton(card.getTerm(), Color.parseColor("#005AAE"), Color.WHITE);
            btn.setTag("term:" + card.getFlashcardId());
            btn.setOnClickListener(v -> onLeftClick(btn, card.getFlashcardId(), card.getTerm()));
            layoutLeftColumn.addView(btn);
            layoutLeftColumn.addView(spacer());
        }

        for (SessionCardResponse card : rightOrder) {
            String def = card.getDefinition() != null ? card.getDefinition() : "";
            Button btn = makeButton(def, Color.parseColor("#F1F5F9"), Color.parseColor("#1e293b"));
            btn.setTag("def:" + card.getFlashcardId());
            btn.setOnClickListener(v -> onRightClick(btn, card.getFlashcardId(), card.getTerm()));
            layoutRightColumn.addView(btn);
            layoutRightColumn.addView(spacer());
        }

        updateProgress();
    }

    private void onLeftClick(Button btn, String cardId, String term) {
        if (btn.isEnabled() == false) return;
        // Deselect previous
        if (selectedLeft != null) resetButtonColor(selectedLeft, true);
        selectedLeft    = btn;
        selectedLeftId  = cardId;
        btn.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(Color.parseColor("#FFC107")));
        tryMatch();
    }

    private void onRightClick(Button btn, String cardId, String term) {
        if (!btn.isEnabled()) return;
        if (selectedRight != null) resetButtonColor(selectedRight, false);
        selectedRight     = btn;
        selectedRightTerm = cardId; // reusing field as cardId for matching
        btn.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(Color.parseColor("#FFC107")));
        tryMatch();
    }

    private void tryMatch() {
        if (selectedLeft == null || selectedRight == null) return;

        // Match if both cards have same ID
        String leftId  = selectedLeftId;
        String rightId = selectedRightTerm; // actually cardId

        Button left  = selectedLeft;
        Button right = selectedRight;
        selectedLeft = null; selectedRight = null;
        selectedLeftId = null; selectedRightTerm = null;

        if (leftId.equals(rightId)) {
            // Correct match
            left.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            right.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            left.setEnabled(false);
            right.setEnabled(false);
            matchedCount++;
            updateProgress();

            // Remove matched cards from list
            cards.removeIf(c -> c.getFlashcardId().equals(leftId));

            if (cards.isEmpty()) {
                new Handler().postDelayed(this::endSession, 500);
            } else if (matchedCount % MAX_PAIRS == 0) {
                // Next batch
                new Handler().postDelayed(this::buildMatchBoard, 600);
            }
        } else {
            // Wrong
            wrongCount++;
            left.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336")));
            right.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336")));
            new Handler().postDelayed(() -> {
                resetButtonColor(left, true);
                resetButtonColor(right, false);
            }, 500);
        }
    }

    private void resetButtonColor(Button btn, boolean isLeft) {
        if (isLeft) {
            btn.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#005AAE")));
        } else {
            btn.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#F1F5F9")));
        }
    }

    private void updateProgress() {
        if (tvProgress != null)
            tvProgress.setText("Matched: " + matchedCount);
    }

    private void endSession() {
        if (sessionId == null) { finish(); return; }
        List<String> wrongTermsList = new ArrayList<>();
        // Each wrong click = 1 error
        for (int i = 0; i < wrongCount; i++) wrongTermsList.add("error");

        apiService.endSession(sessionId, wrongTermsList)
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
        } else {
            intent.putExtra(SessionResultActivity.EXTRA_CORRECT, matchedCount);
            intent.putExtra(SessionResultActivity.EXTRA_TOTAL,   matchedCount + wrongCount);
        }
        intent.putExtra(SessionResultActivity.EXTRA_MODE, "Match the Pairs");
        startActivity(intent);
        finish();
    }

    private Button makeButton(String text, int bgColor, int textColor) {
        Button btn = new Button(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(44));
        btn.setLayoutParams(lp);
        btn.setText(text);
        btn.setTextColor(textColor);
        btn.setTextSize(12f);
        btn.setMaxLines(2);
        btn.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(bgColor));
        return btn;
    }

    private View spacer() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(6)));
        return v;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void confirmExit() {
        new AlertDialog.Builder(this)
                .setTitle("Thoát khỏi game?")
                .setPositiveButton("Thoát", (d, w) -> finish())
                .setNegativeButton("Tiếp tục", null).show();
    }
}
