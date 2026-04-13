package com.estudy.app.controller;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.estudy.app.R;
import com.estudy.app.api.ApiClient;
import com.estudy.app.api.ApiService;
import com.estudy.app.model.request.AnswerRequest;
import com.estudy.app.model.response.ApiResponse;
import com.estudy.app.model.response.FlashCardResponse;
import com.estudy.app.model.response.FlashCardSetDetailResponse;
import com.estudy.app.utils.TokenManager;
import com.estudy.app.utils.WrapFlowLayout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.*;
import java.util.HashSet;
import java.util.Set;

public class SpellingActivity extends AppCompatActivity {

    public static final String EXTRA_SET_ID   = "setId";
    public static final String EXTRA_SET_NAME = "setName";

    private static final int MAX_HINTS = 3;

    // Progress
    private ProgressBar  progressBar;
    private TextView     tvCurrentNum, tvTotalNum, tvWrongCount, tvCorrectCount;

    // Question
    private TextView     tvDefinition, tvIpa;
    private ImageButton  btnSpeaker;

    // Hint box (FEE685)
    private LinearLayout layoutHintBox;
    private TextView     tvHintDisplay;   // hiện dashes / revealed / "✓ Correct!" / "rular ✗"
    private ImageView    ivHintEye;

    // Answer area (DFE7EF → xanh / đỏ sau check)
    private LinearLayout  layoutAnswerArea;
    private TextView      tvLetterCount;
    private WrapFlowLayout layoutLetterBoxes;
    private TextView      tvCorrectAnswerLabel, tvCorrectAnswer;

    // Hidden input
    private EditText etHiddenInput;

    // Button
    private Button btnCheck;

    // Data
    private ApiService   apiService;
    private TextToSpeech tts;
    private String       setId;
    private List<FlashCardResponse> cards = new ArrayList<>();
    private int currentIndex = 0;
    private int correctCount = 0;
    private int wrongCount   = 0;
    private int hintCount    = 0;
    private int hintLevel    = 0;
    private boolean answered      = false;
    private boolean hintRevealed   = false;
    private final Set<Integer> revealedPositions = new HashSet<>();

    private List<TextView> letterBoxes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spelling);

        setId = getIntent().getStringExtra(EXTRA_SET_ID);
        apiService = ApiClient.getInstance(new TokenManager(this)).create(ApiService.class);
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) tts.setLanguage(Locale.US);
        });

        bindViews();
        loadCards();
    }

    @Override protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }

    private void bindViews() {
        progressBar   = findViewById(R.id.progressBar);
        tvCurrentNum  = findViewById(R.id.tvCurrentNum);
        tvTotalNum    = findViewById(R.id.tvTotalNum);
        tvWrongCount  = findViewById(R.id.tvWrongCount);
        tvCorrectCount = findViewById(R.id.tvCorrectCount);

        tvDefinition  = findViewById(R.id.tvDefinition);
        tvIpa         = findViewById(R.id.tvIpa);
        btnSpeaker    = findViewById(R.id.btnSpeaker);

        layoutHintBox  = findViewById(R.id.layoutHintBox);
        tvHintDisplay  = findViewById(R.id.tvHintDisplay);
        ivHintEye      = findViewById(R.id.ivHintEye);

        layoutAnswerArea     = findViewById(R.id.layoutAnswerArea);
        tvLetterCount        = findViewById(R.id.tvLetterCount);
        layoutLetterBoxes    = findViewById(R.id.layoutLetterBoxes);
        tvCorrectAnswerLabel = findViewById(R.id.tvCorrectAnswerLabel);
        tvCorrectAnswer      = findViewById(R.id.tvCorrectAnswer);

        etHiddenInput = findViewById(R.id.etHiddenInput);
        btnCheck      = findViewById(R.id.btnCheck);

        // Back / Settings
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> confirmExit());
        ImageButton btnSettings = findViewById(R.id.btnSettings);
        if (btnSettings != null) btnSettings.setOnClickListener(v -> confirmExit());

        // Hint box click
        if (layoutHintBox != null) layoutHintBox.setOnClickListener(v -> revealHint());

        // Speaker
        if (btnSpeaker != null) btnSpeaker.setOnClickListener(v -> {
            if (tts != null && !TextUtils.isEmpty(getCurrentTerm()))
                tts.speak(getCurrentTerm(), TextToSpeech.QUEUE_FLUSH, null, "tts");
        });

        // Check / Next
        if (btnCheck != null) btnCheck.setOnClickListener(v -> {
            if (answered) moveNext();
            else checkAnswer();
        });

        // Keyboard → update letter boxes + hint display
        if (etHiddenInput != null) {
            etHiddenInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int i, int b, int c) {}
                @Override public void afterTextChanged(Editable s) {
                    if (!answered) {
                        updateLetterBoxes(s.toString());
                        updateHintDashes(s.toString());
                    }
                }
            });
        }
    }

    private void loadCards() {
        apiService.getFlashCardSetDetail(setId)
                .enqueue(new Callback<ApiResponse<FlashCardSetDetailResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<FlashCardSetDetailResponse>> call,
                                           Response<ApiResponse<FlashCardSetDetailResponse>> res) {
                        if (res.isSuccessful() && res.body() != null
                                && res.body().getResult() != null) {
                            List<FlashCardResponse> all = res.body().getResult().getFlashCards();
                            if (all != null && !all.isEmpty()) {
                                cards = new ArrayList<>(all);
                                Collections.shuffle(cards);
                                if (tvTotalNum != null)
                                    tvTotalNum.setText(String.valueOf(cards.size()));
                                showCard(0);
                            } else {
                                Toast.makeText(SpellingActivity.this,
                                        "Không có từ để học", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        } else {
                            Toast.makeText(SpellingActivity.this,
                                    "Không tải được dữ liệu", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                    @Override public void onFailure(Call<ApiResponse<FlashCardSetDetailResponse>> c, Throwable t) {
                        Toast.makeText(SpellingActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    // ── Show card ─────────────────────────────────────────────
    private void showCard(int index) {
        if (index >= cards.size()) { openResult(); return; }
        currentIndex = index;
        answered     = false;
        hintCount    = 0;
        hintLevel    = 0;
        revealedPositions.clear();

        FlashCardResponse card = cards.get(index);
        String term = card.getTerm() != null ? card.getTerm() : "";

        // Progress
        if (progressBar != null) progressBar.post(() -> {
            int barW   = progressBar.getWidth();
            int badgeW = tvCurrentNum != null ? tvCurrentNum.getWidth() : 0;
            if (barW > 0 && cards.size() > 0) {
                // fill đến hết rìa phải hình tròn
                float fraction = (float) index / cards.size() + (float) badgeW / barW;
                progressBar.setProgress((int) Math.min(100, fraction * 100));
            }
        });

        if (tvCurrentNum != null) tvCurrentNum.setText(String.valueOf(index + 1));
        moveBadge(index + 1, cards.size());

        // Question
        if (tvDefinition != null) tvDefinition.setText(card.getDefinition() != null ? card.getDefinition() : "");
        if (tvIpa != null) tvIpa.setText(card.getIpa() != null ? card.getIpa() : "");

        // Reset hint box: nền vàng + dashes + eye icon
        resetHintBox(term);

        // Reset answer area: nền DFE7EF
        resetAnswerArea(term);

        // Reset input
        if (etHiddenInput != null) etHiddenInput.setText("");

        // Reset button
        if (btnCheck != null) { btnCheck.setText("CHECK"); btnCheck.setEnabled(true); }

        if (etHiddenInput != null) etHiddenInput.requestFocus();
    }

    // ── Hint box helpers ──────────────────────────────────────

    /**
     * Hint box bình thường: nền FEE685, hiện dashes, có eye icon
     */
    private void resetHintBox(String term) {
        if (layoutHintBox == null) return;
        layoutHintBox.setBackground(getDrawable(R.drawable.bg_hint_button));
        layoutHintBox.setClickable(true);
        layoutHintBox.setAlpha(1f);
        hintRevealed = false;
        // Ban đầu: icon đèn ở layout + chữ "Hint", eye icon ẩn
        if (tvHintDisplay != null) {
            tvHintDisplay.setText("Hint");
            tvHintDisplay.setTextColor(android.graphics.Color.parseColor("#913A00"));
        }
        if (ivHintEye != null) ivHintEye.setVisibility(View.GONE);
    }

    /**
     * Cập nhật dashes trên hint box theo typed + hintLevel
     * Ví dụ term="ruler", typed="ru", hintLevel=1 → "r u _ _ _" (hint reveal r, typed ru)
     */
    private void updateHintDashes(String typed) {
        if (!hintRevealed) return; // chưa nhấn hint lần đầu thì không cập nhật
        updateHintDashesWithReveal(typed);
    }

    private void revealHint() {
        if (answered) return;
        String term = getCurrentTerm();

        // Lần đầu nhấn: đổi sang hiện dashes + eye icon (không return)
        if (!hintRevealed) {
            hintRevealed = true;
            if (ivHintEye != null) ivHintEye.setVisibility(View.VISIBLE);
        }

        // Hết lượt thì chỉ cập nhật UI (dashes vẫn hiện), không reveal thêm
        if (hintCount >= MAX_HINTS) {
            String typed = etHiddenInput != null ? etHiddenInput.getText().toString() : "";
            updateHintDashesWithReveal(typed);
            Toast.makeText(this, "Hết " + MAX_HINTS + " lượt hint!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tìm các vị trí chưa typed VÀ chưa reveal
        String typed = etHiddenInput != null ? etHiddenInput.getText().toString() : "";
        List<Integer> unrevealed = new ArrayList<>();
        for (int i = 0; i < term.length(); i++) {
            if (i >= typed.length() && !revealedPositions.contains(i))
                unrevealed.add(i);
        }

        if (!unrevealed.isEmpty()) {
            // Reveal 1 vị trí ngẫu nhiên
            int randomIdx = unrevealed.get(new Random().nextInt(unrevealed.size()));
            revealedPositions.add(randomIdx);
            hintCount++;
        }

        // Cập nhật hint box + letter boxes
        String currentTyped = etHiddenInput != null ? etHiddenInput.getText().toString() : "";
        updateHintDashesWithReveal(currentTyped);

        if (hintCount >= MAX_HINTS && layoutHintBox != null)
            layoutHintBox.setAlpha(0.5f);
    }

    // ── Answer area helpers ───────────────────────────────────

    private void resetAnswerArea(String term) {
        if (layoutAnswerArea != null)
            layoutAnswerArea.setBackground(getDrawable(R.drawable.bg_answer_area));
        if (tvLetterCount != null)
            tvLetterCount.setText("Number of letters: " + term.length());
        if (tvCorrectAnswerLabel != null) tvCorrectAnswerLabel.setVisibility(View.GONE);
        if (tvCorrectAnswer != null)      tvCorrectAnswer.setVisibility(View.GONE);
        buildLetterBoxes(term);
    }

    private void buildLetterBoxes(String term) {
        letterBoxes.clear();
        if (layoutLetterBoxes == null) return;
        layoutLetterBoxes.removeAllViews();

        int boxSize = dp(42);
        layoutLetterBoxes.setGap(dp(6), dp(6));

        for (int i = 0; i < term.length(); i++) {
            TextView box = new TextView(this);
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(boxSize, boxSize);
            box.setLayoutParams(lp);
            box.setGravity(Gravity.CENTER);
            box.setTextSize(16f);
            box.setTextColor(Color.parseColor("#1e293b"));
            box.setBackground(getDrawable(R.drawable.bg_letter_box));
            box.setText("");
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
            } else if (i < hintLevel && i < term.length()) {
                letterBoxes.get(i).setText(String.valueOf(term.charAt(i)));
                letterBoxes.get(i).setBackground(getDrawable(R.drawable.bg_hint_button));
            } else {
                letterBoxes.get(i).setText("");
                letterBoxes.get(i).setBackground(getDrawable(R.drawable.bg_letter_box));
            }
        }
    }

    // ── Check answer ──────────────────────────────────────────
    private void checkAnswer() {
        String typed = etHiddenInput != null ? etHiddenInput.getText().toString().trim() : "";
        String term  = getCurrentTerm();
        boolean correct = typed.equalsIgnoreCase(term.trim());

        answered = true;
        if (btnCheck != null) btnCheck.setText("NEXT");

        // Hint box: hiện kết quả
        if (layoutHintBox != null) {
            layoutHintBox.setClickable(false);
            if (correct) {
                // ✓ Correct ! — nền xanh
                layoutHintBox.setBackground(getDrawable(R.drawable.bg_correct_feedback));
                if (tvHintDisplay != null) {
                    tvHintDisplay.setText("✓  Correct !");
                    tvHintDisplay.setTextColor(Color.parseColor("#4FB968"));
                }
            } else {
                // "rular  ✗" — nền đỏ
                layoutHintBox.setBackground(getDrawable(R.drawable.bg_wrong_feedback));
                if (tvHintDisplay != null) {
                    tvHintDisplay.setText(typed + "  ✗");
                    tvHintDisplay.setTextColor(Color.parseColor("#FB2C36"));
                }
            }
            if (ivHintEye != null) ivHintEye.setVisibility(View.GONE);
        }

        // Answer area: đổi nền + hiện đáp án đúng nếu sai
        if (correct) {
            correctCount++;
            if (layoutAnswerArea != null)
                layoutAnswerArea.setBackground(getDrawable(R.drawable.bg_answer_correct));
            // Hiện từ đúng trong ô — đổi màu chữ các box thành xanh
            for (TextView box : letterBoxes) {
                box.setTextColor(Color.parseColor("#4FB968"));
                box.setBackground(getDrawable(R.drawable.bg_letter_box));
            }
        } else {
            wrongCount++;
            if (layoutAnswerArea != null)
                layoutAnswerArea.setBackground(getDrawable(R.drawable.bg_answer_wrong));
            // Hiện đáp án đúng bên dưới
            if (tvCorrectAnswerLabel != null) {
                tvCorrectAnswerLabel.setVisibility(View.VISIBLE);
            }
            if (tvCorrectAnswer != null) {
                tvCorrectAnswer.setText(term);
                tvCorrectAnswer.setVisibility(View.VISIBLE);
            }
            // Đổi chữ box thành đỏ
            for (TextView box : letterBoxes) {
                box.setTextColor(Color.parseColor("#FB2C36"));
                box.setBackground(getDrawable(R.drawable.bg_letter_box));
            }
        }

        updateCounters();
        submitAnswer(correct);
    }

    private void moveNext() { showCard(currentIndex + 1); }

    private void updateCounters() {
        if (tvWrongCount != null)  tvWrongCount.setText("F: " + wrongCount);
        if (tvCorrectCount != null) tvCorrectCount.setText("T: " + correctCount);
    }

    private void submitAnswer(boolean correct) {
        if (currentIndex >= cards.size()) return;
        FlashCardResponse card = cards.get(currentIndex);
        AnswerRequest req = new AnswerRequest(card.getId(), null, null, correct);
        apiService.submitAnswer(req).enqueue(new Callback<ApiResponse<Void>>() {
            @Override public void onResponse(Call<ApiResponse<Void>> c, Response<ApiResponse<Void>> r) {}
            @Override public void onFailure(Call<ApiResponse<Void>> c, Throwable t) {}
        });
    }

    /**
     * Cập nhật dashes khi hint random vị trí:
     * - Ô đã typed → hiện ký tự typed
     * - Ô đã reveal (revealedPositions) → hiện ký tự term, màu khác
     * - Còn lại → "_"
     */
    private void updateHintDashesWithReveal(String typed) {
        String term = getCurrentTerm();
        if (term.isEmpty() || tvHintDisplay == null) return;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < term.length(); i++) {
            if (i < typed.length()) {
                sb.append(typed.charAt(i));
            } else if (revealedPositions.contains(i)) {
                sb.append(term.charAt(i));
            } else {
                sb.append('_');
            }
            if (i < term.length() - 1) sb.append(' ');
        }
        tvHintDisplay.setText(sb.toString());
        tvHintDisplay.setTextColor(android.graphics.Color.parseColor("#913A00"));

        // Cũng update letter boxes
        updateLetterBoxesWithReveal(typed);
    }

    private void updateLetterBoxesWithReveal(String typed) {
        String term = getCurrentTerm();
        for (int i = 0; i < letterBoxes.size(); i++) {
            if (i < typed.length()) {
                letterBoxes.get(i).setText(String.valueOf(typed.charAt(i)));
                letterBoxes.get(i).setBackground(getDrawable(R.drawable.bg_letter_box_filled));
            } else if (revealedPositions.contains(i)) {
                letterBoxes.get(i).setText(String.valueOf(term.charAt(i)));
                letterBoxes.get(i).setBackground(getDrawable(R.drawable.bg_hint_button));
            } else {
                letterBoxes.get(i).setText("");
                letterBoxes.get(i).setBackground(getDrawable(R.drawable.bg_letter_box));
            }
        }
    }

    private void openResult() {
        Intent intent = new Intent(this, SessionResultActivity.class);
        intent.putExtra(SessionResultActivity.EXTRA_CORRECT, correctCount);
        intent.putExtra(SessionResultActivity.EXTRA_TOTAL, correctCount + wrongCount);
        intent.putExtra(SessionResultActivity.EXTRA_MODE, "Spelling");
        startActivity(intent);
        finish();
    }

    // ── Progress badge ────────────────────────────────────────
    private void moveBadge(int current, int total) {
        if (tvCurrentNum == null || progressBar == null) return;
        progressBar.post(() -> {
            int barW   = progressBar.getWidth();
            int badgeW = tvCurrentNum.getWidth();
            if (barW == 0 || badgeW == 0) return;
            float xPos = progressBar.getLeft() + barW * ((float) current / total) - badgeW / 2f;
            xPos = Math.max(0, Math.min(xPos, progressBar.getLeft() + barW - badgeW));
            tvCurrentNum.setTranslationX(xPos - tvCurrentNum.getLeft());
        });
    }

    private String getCurrentTerm() {
        if (currentIndex < cards.size()) {
            String t = cards.get(currentIndex).getTerm();
            return t != null ? t : "";
        }
        return "";
    }

    private void confirmExit() {
        new AlertDialog.Builder(this)
                .setTitle("Thoát?")
                .setPositiveButton("Thoát", (d, w) -> finish())
                .setNegativeButton("Tiếp tục", null).show();
    }

    private int dp(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }
}