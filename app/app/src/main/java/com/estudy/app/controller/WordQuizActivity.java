package com.estudy.app.controller;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
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
import java.util.Random;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class WordQuizActivity extends AppCompatActivity {

    public static final String EXTRA_SET_ID   = "set_id";
    public static final String EXTRA_SET_NAME = "set_name";

    // Header
    private TextView tvSetName;

    // Progress
    private TextView    tvCurrentNum, tvTotalNum, tvFailCount, tvTotalAnswered;
    private ProgressBar progressBar;

    // Question
    private TextView    tvQuestion, tvQuestionIpa;
    private ImageButton btnPlayAudio;

    // Options (LinearLayout containers)
    private LinearLayout btnOption1, btnOption2, btnOption3, btnOption4;
    private TextView     tvOption1, tvOption2, tvOption3, tvOption4;
    private TextView     tvLabel1,  tvLabel2,  tvLabel3,  tvLabel4;

    // Next
    private Button btnNext;

    // Data
    private ApiService  apiService;
    private TextToSpeech tts;
    private String      sessionId;
    private List<SessionCardResponse> cards = new ArrayList<>();
    private int         currentIndex  = 0;
    private int         failCount     = 0;
    private int         totalAnswered = 0;
    private final List<String> wrongTerms = new ArrayList<>();

    // Options text (để kiểm tra đáp án đúng)
    private final String[] optionTexts = new String[4];

    // Mode câu hỏi: true = EN→VI (hỏi term, đáp definition), false = VI→EN
    private boolean isEnToVi = true;

    // Label hướng dẫn
    private TextView tvQuestionLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_quiz);

        apiService = ApiClient.getInstance(new TokenManager(this)).create(ApiService.class);
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) tts.setLanguage(Locale.US);
        });

        bindViews();
        startSession(getIntent().getStringExtra(EXTRA_SET_ID));
    }

    @Override
    protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }

    private void bindViews() {
        tvSetName = findViewById(R.id.tvSetName);
        if (tvSetName != null) {
            String name = getIntent().getStringExtra(EXTRA_SET_NAME);
            tvSetName.setText(name != null ? name : "Word Quiz");
        }

        tvCurrentNum    = findViewById(R.id.tvCurrentNum);
        tvTotalNum      = findViewById(R.id.tvTotalNum);
        tvFailCount     = findViewById(R.id.tvFailCount);
        tvTotalAnswered = findViewById(R.id.tvTotalAnswered);
        progressBar     = findViewById(R.id.progressBar);

        tvQuestionLabel = findViewById(R.id.tvQuestionLabel);
        tvQuestion    = findViewById(R.id.tvQuestion);
        tvQuestionIpa = findViewById(R.id.tvQuestionIpa);
        btnPlayAudio  = findViewById(R.id.btnPlayAudio);

        btnOption1 = findViewById(R.id.btnOption1);
        btnOption2 = findViewById(R.id.btnOption2);
        btnOption3 = findViewById(R.id.btnOption3);
        btnOption4 = findViewById(R.id.btnOption4);

        tvOption1 = findViewById(R.id.tvOption1);
        tvOption2 = findViewById(R.id.tvOption2);
        tvOption3 = findViewById(R.id.tvOption3);
        tvOption4 = findViewById(R.id.tvOption4);

        tvLabel1 = findViewById(R.id.tvLabel1);
        tvLabel2 = findViewById(R.id.tvLabel2);
        tvLabel3 = findViewById(R.id.tvLabel3);
        tvLabel4 = findViewById(R.id.tvLabel4);

        btnNext = findViewById(R.id.btnNext);

        // Back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> confirmExit());
        ImageButton btnSettings = findViewById(R.id.btnSettings);
        if (btnSettings != null) btnSettings.setOnClickListener(v -> confirmExit());

        // Option click listeners
        View.OnClickListener optionListener = v -> {
            int idx = -1;
            if (v == btnOption1) idx = 0;
            else if (v == btnOption2) idx = 1;
            else if (v == btnOption3) idx = 2;
            else if (v == btnOption4) idx = 3;
            if (idx >= 0) checkAnswer(idx);
        };
        if (btnOption1 != null) btnOption1.setOnClickListener(optionListener);
        if (btnOption2 != null) btnOption2.setOnClickListener(optionListener);
        if (btnOption3 != null) btnOption3.setOnClickListener(optionListener);
        if (btnOption4 != null) btnOption4.setOnClickListener(optionListener);

        // Next button
        if (btnNext != null) btnNext.setOnClickListener(v -> showQuestion(currentIndex + 1));
    }

    private void startSession(String setId) {
        apiService.startSession(new StartSessionRequest(setId, "word_quiz"))
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
                                Toast.makeText(WordQuizActivity.this,
                                        "Không có câu hỏi nào!", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                if (tvTotalNum != null)
                                    tvTotalNum.setText(String.valueOf(cards.size()));
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

        // Update progress
        if (tvCurrentNum != null) tvCurrentNum.setText(String.valueOf(index + 1));
        if (tvTotalNum != null)   tvTotalNum.setText(String.valueOf(total));
        if (progressBar != null)  progressBar.setProgress((index + 1) * 100 / total);
        updateCounters();
        // Di chuyển badge theo thanh progress
        moveBadgeToProgress(index + 1, total);

        // Random EN→VI hoặc VI→EN mỗi câu
        isEnToVi = new Random().nextBoolean();

        String term = card.getTerm() != null ? card.getTerm() : "";
        String def  = card.getDefinition() != null ? card.getDefinition() : "";
        String ipa  = card.getIpa() != null ? card.getIpa() : "";

        if (isEnToVi) {
            // Hỏi: từ tiếng Anh → chọn nghĩa tiếng Việt
            if (tvQuestionLabel != null) tvQuestionLabel.setText("Choose the correct meaning");
            if (tvQuestion != null)    tvQuestion.setText(term);
            if (tvQuestionIpa != null) tvQuestionIpa.setText(ipa);
            if (tvQuestionIpa != null) tvQuestionIpa.setVisibility(android.view.View.VISIBLE);
        } else {
            // Hỏi: nghĩa tiếng Việt → chọn từ tiếng Anh
            if (tvQuestionLabel != null) tvQuestionLabel.setText("Choose the correct word");
            if (tvQuestion != null)    tvQuestion.setText(def);
            if (tvQuestionIpa != null) tvQuestionIpa.setVisibility(android.view.View.GONE);
        }

        // Audio (luôn đọc term tiếng Anh)
        if (btnPlayAudio != null) btnPlayAudio.setOnClickListener(v -> speakTerm(term));

        // Build options: nếu EN→VI thì đáp án là definition, VI→EN thì đáp án là term
        List<String> options = new ArrayList<>();
        String correctAnswer;

        if (isEnToVi) {
            // Đáp án đúng = definition
            correctAnswer = def;
            options.add(correctAnswer);
            List<String> distractors = card.getDistractors();
            if (distractors != null) {
                for (int i = 0; i < Math.min(3, distractors.size()); i++)
                    options.add(distractors.get(i));
            }
        } else {
            // Đáp án đúng = term tiếng Anh
            // Distractors = term của các card khác trong batch
            correctAnswer = term;
            options.add(correctAnswer);
            // Lấy term ngẫu nhiên từ các card khác làm distractor
            List<String> otherTerms = new ArrayList<>();
            for (SessionCardResponse other : cards) {
                if (!other.getFlashcardId().equals(card.getFlashcardId())
                        && other.getTerm() != null) {
                    otherTerms.add(other.getTerm());
                }
            }
            Collections.shuffle(otherTerms);
            for (int i = 0; i < Math.min(3, otherTerms.size()); i++)
                options.add(otherTerms.get(i));
        }

        while (options.size() < 4) options.add("—");
        Collections.shuffle(options);

        // Set text
        TextView[] tvOpts = {tvOption1, tvOption2, tvOption3, tvOption4};
        for (int i = 0; i < 4; i++) {
            optionTexts[i] = options.get(i);
            if (tvOpts[i] != null) tvOpts[i].setText(optionTexts[i]);
        }

        // Reset styles
        resetAllOptions();
        setOptionsEnabled(true);

        // Hide next button
        if (btnNext != null) btnNext.setVisibility(View.GONE);
    }

    private void checkAnswer(int selectedIdx) {
        if (currentIndex >= cards.size()) return;
        setOptionsEnabled(false);

        SessionCardResponse card = cards.get(currentIndex);
        // Đáp án đúng tùy mode
        String correctAnswer = isEnToVi
                ? (card.getDefinition() != null ? card.getDefinition() : "")
                : (card.getTerm() != null ? card.getTerm() : "");
        boolean isCorrect = correctAnswer.equals(optionTexts[selectedIdx]);

        totalAnswered++;
        if (!isCorrect) {
            failCount++;
            wrongTerms.add(card.getTerm());
        }
        updateCounters();

        // Highlight options
        LinearLayout[] opts = {btnOption1, btnOption2, btnOption3, btnOption4};
        TextView[] labels   = {tvLabel1, tvLabel2, tvLabel3, tvLabel4};

        for (int i = 0; i < 4; i++) {
            if (correctAnswer.equals(optionTexts[i])) {
                setOptionStyle(opts[i], labels[i], "correct");
            } else if (i == selectedIdx && !isCorrect) {
                setOptionStyle(opts[i], labels[i], "wrong");
            }
        }

        // Submit answer to backend
        AnswerRequest req = new AnswerRequest(card.getFlashcardId(), sessionId, null, isCorrect);
        apiService.submitAnswer(req).enqueue(new Callback<ApiResponse<Void>>() {
            @Override public void onResponse(Call<ApiResponse<Void>> c, Response<ApiResponse<Void>> r) {}
            @Override public void onFailure(Call<ApiResponse<Void>> c, Throwable t) {}
        });

        // Hiện nút NEXT thay vì auto-next
        if (btnNext != null) btnNext.setVisibility(View.VISIBLE);
    }

    private void setOptionStyle(LinearLayout container, TextView label, String type) {
        if (container == null) return;
        switch (type) {
            case "correct":
                container.setBackgroundResource(R.drawable.bg_option_correct);
                if (label != null) {
                    // Nền xanh 4FB968, chữ trắng
                    label.setTextColor(android.graphics.Color.WHITE);
                    label.setBackground(getDrawable(R.drawable.bg_label_circle_correct));
                }
                break;
            case "wrong":
                container.setBackgroundResource(R.drawable.bg_option_wrong);
                if (label != null) {
                    // Nền đỏ FB2C36, chữ trắng
                    label.setTextColor(android.graphics.Color.WHITE);
                    label.setBackground(getDrawable(R.drawable.bg_label_circle_wrong));
                }
                break;
        }
    }

    private void resetAllOptions() {
        LinearLayout[] opts   = {btnOption1, btnOption2, btnOption3, btnOption4};
        TextView[] labels     = {tvLabel1, tvLabel2, tvLabel3, tvLabel4};
        String[] labelTexts   = {"A", "B", "C", "D"};

        for (int i = 0; i < 4; i++) {
            if (opts[i] != null) opts[i].setBackgroundResource(R.drawable.bg_option_default);
            if (labels[i] != null) {
                labels[i].setText(labelTexts[i]);
                // Default: nền D9D9D9, chữ đen
                labels[i].setTextColor(android.graphics.Color.parseColor("#333333"));
                labels[i].setBackgroundResource(R.drawable.bg_label_circle);
            }
        }
    }

    private void setOptionsEnabled(boolean enabled) {
        LinearLayout[] opts = {btnOption1, btnOption2, btnOption3, btnOption4};
        for (LinearLayout opt : opts) if (opt != null) opt.setEnabled(enabled);
    }

    private void updateCounters() {
        if (tvFailCount != null)
            tvFailCount.setText(String.format(Locale.getDefault(), "F: %d", failCount));
        if (tvTotalAnswered != null)
            tvTotalAnswered.setText(String.format(Locale.getDefault(), "T: %d", totalAnswered));
    }

    private void speakTerm(String term) {
        if (tts != null && !TextUtils.isEmpty(term))
            tts.speak(term, TextToSpeech.QUEUE_FLUSH, null, "tts");
    }

    private void moveBadgeToProgress(int current, int total) {
        if (tvCurrentNum == null || progressBar == null) return;
        progressBar.post(() -> {
            int barWidth  = progressBar.getWidth();
            int badgeWidth = tvCurrentNum.getWidth();
            if (barWidth == 0 || badgeWidth == 0) return;

            // Vị trí left của progressBar so với parent ConstraintLayout
            int barLeft = progressBar.getLeft();
            // % tiến độ → pixel position trên bar
            float ratio = (float) current / total;
            float xPos  = barLeft + barWidth * ratio - badgeWidth / 2f;
            // Giữ badge trong bounds
            xPos = Math.max(0, Math.min(xPos, barLeft + barWidth - badgeWidth));
            tvCurrentNum.setTranslationX(xPos - tvCurrentNum.getLeft());
        });
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
            intent.putExtra(SessionResultActivity.EXTRA_CORRECT,    result.getCorrectCount());
            intent.putExtra(SessionResultActivity.EXTRA_TOTAL,      result.getTotalQuestions());
            intent.putExtra(SessionResultActivity.EXTRA_STREAK,     result.getCurrentStreak());
            intent.putExtra(SessionResultActivity.EXTRA_NEW_RECORD, result.isNewRecord());
            intent.putExtra(SessionResultActivity.EXTRA_DURATION,   result.getDurationSeconds());
        } else {
            intent.putExtra(SessionResultActivity.EXTRA_CORRECT, totalAnswered - failCount);
            intent.putExtra(SessionResultActivity.EXTRA_TOTAL,   cards.size());
        }
        intent.putExtra(SessionResultActivity.EXTRA_MODE, "Word Quiz");
        startActivity(intent);
        finish();
    }

    private void confirmExit() {
        new AlertDialog.Builder(this)
                .setTitle("Thoát khỏi quiz?")
                .setMessage("Kết quả hiện tại sẽ được lưu lại.")
                .setPositiveButton("Thoát", (d, w) -> {
                    if (sessionId != null) {
                        apiService.endSession(sessionId, wrongTerms).enqueue(
                                new Callback<ApiResponse<SessionResultResponse>>() {
                                    @Override public void onResponse(Call<ApiResponse<SessionResultResponse>> c, Response<ApiResponse<SessionResultResponse>> r) { finish(); }
                                    @Override public void onFailure(Call<ApiResponse<SessionResultResponse>> c, Throwable t) { finish(); }
                                });
                    } else finish();
                })
                .setNegativeButton("Tiếp tục", null).show();
    }
}