package com.estudy.app.controller;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
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

public class SpellingActivity extends AppCompatActivity {

    public static final String EXTRA_SET_ID = "setId";
    public static final String EXTRA_SET_NAME = "setName";

    private ProgressBar progressBar;
    private TextView tvWrongCount, tvCorrectCount, tvDefinition, tvIpa, tvLetterCount;
    private ImageButton btnBack, btnSpeaker;
    private Button btnHint, btnCheck;
    private LinearLayout layoutLetterBoxes, layoutFeedback;
    private LinearLayout layoutCorrectFeedback, layoutWrongFeedback;
    private TextView tvUserAnswer, tvCorrectAnswer, tvCorrectWord;
    private EditText etHiddenInput;

    private ApiService apiService;
    private String setId;

    private List<FlashCardResponse> cards = new ArrayList<>();
    private int currentIndex = 0;
    private int correctCount = 0;
    private int wrongCount = 0;
    private int hintLevel = 0;
    private boolean answered = false;

    // Letter boxes views
    private List<TextView> letterBoxes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spelling);

        setId = getIntent().getStringExtra(EXTRA_SET_ID);
        String setName = getIntent().getStringExtra(EXTRA_SET_NAME);

        TokenManager tokenManager = new TokenManager(this);
        apiService = ApiClient.getInstance(tokenManager).create(ApiService.class);

        bindViews();
        loadCards();
    }

    private void bindViews() {
        progressBar = findViewById(R.id.progressBar);
        tvWrongCount = findViewById(R.id.tvWrongCount);
        tvCorrectCount = findViewById(R.id.tvCorrectCount);
        tvDefinition = findViewById(R.id.tvDefinition);
        tvIpa = findViewById(R.id.tvIpa);
        tvLetterCount = findViewById(R.id.tvLetterCount);
        btnBack = findViewById(R.id.btnBack);
        btnSpeaker = findViewById(R.id.btnSpeaker);
        btnHint = findViewById(R.id.btnHint);
        btnCheck = findViewById(R.id.btnCheck);
        layoutLetterBoxes = findViewById(R.id.layoutLetterBoxes);
        layoutFeedback = findViewById(R.id.layoutFeedback);
        layoutCorrectFeedback = findViewById(R.id.layoutCorrectFeedback);
        layoutWrongFeedback = findViewById(R.id.layoutWrongFeedback);
        tvUserAnswer = findViewById(R.id.tvUserAnswer);
        tvCorrectAnswer = findViewById(R.id.tvCorrectAnswer);
        tvCorrectWord = findViewById(R.id.tvCorrectWord);
        etHiddenInput = findViewById(R.id.etHiddenInput);

        btnBack.setOnClickListener(v -> finish());
        btnHint.setOnClickListener(v -> revealHint());
        btnCheck.setOnClickListener(v -> {
            if (answered) {
                moveNext();
            } else {
                checkAnswer();
            }
        });

        btnSpeaker.setOnClickListener(v ->
                Toast.makeText(this, "Audio — coming soon!", Toast.LENGTH_SHORT).show());

        // As user types, update letter boxes
        etHiddenInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String typed = s.toString();
                updateLetterBoxes(typed);
            }
        });
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
                        cards = new ArrayList<>(all);
                        Collections.shuffle(cards);
                        showCard(0);
                    } else {
                        Toast.makeText(SpellingActivity.this, "Không có từ để học", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    Toast.makeText(SpellingActivity.this, "Không tải được dữ liệu", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<FlashCardSetDetailResponse>> call, Throwable t) {
                Toast.makeText(SpellingActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void showCard(int index) {
        if (index >= cards.size()) {
            openResult();
            return;
        }
        currentIndex = index;
        answered = false;
        hintLevel = 0;

        FlashCardResponse card = cards.get(index);

        // Update progress
        int pct = (int) ((float) index / cards.size() * 100);
        progressBar.setProgress(pct);

        tvDefinition.setText(card.getDefinition() != null ? card.getDefinition() : "");
        tvIpa.setText(card.getIpa() != null ? ("/" + card.getIpa() + "/") : "");

        String term = card.getTerm() != null ? card.getTerm() : "";
        tvLetterCount.setText("Number of letters: " + term.length());

        // Reset
        etHiddenInput.setText("");
        layoutFeedback.setVisibility(View.GONE);
        layoutCorrectFeedback.setVisibility(View.GONE);
        layoutWrongFeedback.setVisibility(View.GONE);
        tvCorrectWord.setVisibility(View.GONE);
        btnHint.setVisibility(View.VISIBLE);
        btnHint.setText("  Hint");
        btnCheck.setText("CHECK");
        btnCheck.setEnabled(true);

        buildLetterBoxes(term);
        // Pre-fill first letter as hint
        if (!term.isEmpty()) {
            etHiddenInput.setText(String.valueOf(term.charAt(0)));
            etHiddenInput.setSelection(etHiddenInput.getText().length());
        }

        // Focus hidden input
        etHiddenInput.requestFocus();
    }

    private void buildLetterBoxes(String term) {
        letterBoxes.clear();
        layoutLetterBoxes.removeAllViews();

        for (int i = 0; i < term.length(); i++) {
            TextView box = new TextView(this);
            int sizePx = dp(40);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
            lp.setMarginEnd(dp(6));
            box.setLayoutParams(lp);
            box.setGravity(Gravity.CENTER);
            box.setTextSize(16f);
            box.setTextColor(Color.parseColor("#1e293b"));
            box.setBackground(getDrawable(R.drawable.bg_letter_box));
            letterBoxes.add(box);
            layoutLetterBoxes.addView(box);
        }
    }

    private void updateLetterBoxes(String typed) {
        String term = getCurrentTerm();
        for (int i = 0; i < letterBoxes.size(); i++) {
            if (i < typed.length()) {
                letterBoxes.get(i).setText(String.valueOf(typed.charAt(i)));
                letterBoxes.get(i).setBackground(getDrawable(R.drawable.bg_letter_box_filled));
            } else {
                // Hint-revealed letters
                if (i < hintLevel && i < term.length()) {
                    letterBoxes.get(i).setText(String.valueOf(term.charAt(i)));
                    letterBoxes.get(i).setBackground(getDrawable(R.drawable.bg_hint_button));
                } else {
                    letterBoxes.get(i).setText("");
                    letterBoxes.get(i).setBackground(getDrawable(R.drawable.bg_letter_box));
                }
            }
        }
    }

    private void revealHint() {
        String term = getCurrentTerm();
        hintLevel = Math.min(hintLevel + 2, term.length());
        // Fill hint letters into hidden input
        String currentTyped = etHiddenInput.getText().toString();
        StringBuilder newTyped = new StringBuilder(currentTyped);
        for (int i = currentTyped.length(); i < hintLevel && i < term.length(); i++) {
            newTyped.append(term.charAt(i));
        }
        etHiddenInput.setText(newTyped.toString());
        etHiddenInput.setSelection(etHiddenInput.getText().length());
        btnHint.setText("  Hint (" + hintLevel + "/" + term.length() + ")");
    }

    private void checkAnswer() {
        String typed = etHiddenInput.getText().toString().trim();
        String term = getCurrentTerm();
        boolean correct = typed.equalsIgnoreCase(term);

        answered = true;
        btnCheck.setText("NEXT");

        layoutFeedback.setVisibility(View.VISIBLE);
        btnHint.setVisibility(View.GONE);

        if (correct) {
            correctCount++;
            layoutCorrectFeedback.setVisibility(View.VISIBLE);
            tvCorrectWord.setVisibility(View.VISIBLE);
            tvCorrectWord.setText(term);
            layoutWrongFeedback.setVisibility(View.GONE);
            updateCounters();
            submitAnswer(true);
        } else {
            wrongCount++;
            layoutWrongFeedback.setVisibility(View.VISIBLE);
            layoutCorrectFeedback.setVisibility(View.GONE);
            tvCorrectWord.setVisibility(View.GONE);
            tvUserAnswer.setText(typed + " ✗");
            tvCorrectAnswer.setText(term);
            updateCounters();
            submitAnswer(false);
        }
    }

    private void moveNext() {
        showCard(currentIndex + 1);
    }

    private void updateCounters() {
        tvCorrectCount.setText("T: " + correctCount);
        tvWrongCount.setText("F: " + wrongCount);
    }

    private void submitAnswer(boolean correct) {
        FlashCardResponse card = cards.get(currentIndex);
        AnswerRequest req = new AnswerRequest(card.getId(), null, null, correct);
        apiService.submitAnswer(req).enqueue(new Callback<ApiResponse<Void>>() {
            @Override public void onResponse(Call<ApiResponse<Void>> c, Response<ApiResponse<Void>> r) {}
            @Override public void onFailure(Call<ApiResponse<Void>> c, Throwable t) {}
        });
    }

    private void openResult() {
        Intent intent = new Intent(this, SessionResultActivity.class);
        intent.putExtra(SessionResultActivity.EXTRA_CORRECT, correctCount);
        intent.putExtra(SessionResultActivity.EXTRA_TOTAL, correctCount + wrongCount);
        intent.putExtra(SessionResultActivity.EXTRA_MODE, "Spelling");
        startActivity(intent);
        finish();
    }

    private String getCurrentTerm() {
        if (currentIndex < cards.size()) {
            String term = cards.get(currentIndex).getTerm();
            return term != null ? term : "";
        }
        return "";
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}