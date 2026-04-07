package com.estudy.app.controller;

import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;
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
import java.util.List;

/**
 * UC-STUDY-05: Spelling — Hiển thị nghĩa, user gõ từ tiếng Anh.
 */
public class SpellingActivity extends AppCompatActivity {

    public static final String EXTRA_SET_ID   = "set_id";
    public static final String EXTRA_SET_NAME = "set_name";

    private TextView    tvProgress, tvMeaning, tvIPA, tvFeedback;
    private EditText    etAnswer;
    private Button      btnCheck, btnHint, btnNext, btnPlayAudio;
    private ProgressBar progressBar;

    private ApiService  apiService;
    private String      sessionId;
    private List<SessionCardResponse> cards = new ArrayList<>();
    private int         currentIndex = 0;
    private int         hintRevealed = 0;
    private final List<String> wrongTerms = new ArrayList<>();

    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spelling);

        apiService = ApiClient.getInstance(new TokenManager(this)).create(ApiService.class);

        // Init TTS
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
            }
        });

        bindViews();
        startSession(getIntent().getStringExtra(EXTRA_SET_ID));
    }

    private void bindViews() {
        tvProgress   = findViewById(R.id.tvProgress);
        tvMeaning    = findViewById(R.id.tvMeaning);
        tvIPA        = findViewById(R.id.tvCardIPA); // reuse id if exists
        tvFeedback   = null; // might not exist in layout — safe null check
        etAnswer     = findViewById(R.id.etAnswer);
        btnCheck     = findViewById(R.id.btnCheck);
        btnHint      = findViewById(R.id.btnHint);
        btnNext      = null; // optional
        progressBar  = findViewById(R.id.progressBar);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> confirmExit());
        ImageButton btnExit = findViewById(R.id.btnExit);
        if (btnExit != null) btnExit.setOnClickListener(v -> confirmExit());

        btnPlayAudio = findViewById(R.id.btnPlayAudio);

        if (btnCheck != null) btnCheck.setOnClickListener(v -> checkAnswer());
        if (btnHint  != null) btnHint.setOnClickListener(v  -> showHint());
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    private void startSession(String setId) {
        showLoading(true);
        apiService.startSession(new StartSessionRequest(setId, "spelling"))
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
                        Toast.makeText(SpellingActivity.this,
                                "Không có từ nào!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        showQuestion(0);
                    }
                } else {
                    Toast.makeText(SpellingActivity.this,
                            "Không thể bắt đầu", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<StartSessionResponse>> call, Throwable t) {
                showLoading(false);
                finish();
            }
        });
    }

    private void showQuestion(int index) {
        if (index >= cards.size()) { endSession(); return; }
        currentIndex = index;
        hintRevealed = 0;

        SessionCardResponse card = cards.get(index);
        int total = cards.size();

        if (tvProgress != null) tvProgress.setText((index + 1) + " / " + total);
        if (progressBar != null) progressBar.setProgress((index + 1) * 100 / total);

        // Show definition
        if (tvMeaning != null)
            tvMeaning.setText(card.getDefinition() != null ? card.getDefinition() : "");

        // IPA
        if (tvIPA != null)
            tvIPA.setText(card.getIpa() != null ? card.getIpa() : "");

        // Clear input
        if (etAnswer != null) {
            etAnswer.setText("");
            etAnswer.setEnabled(true);
            etAnswer.setBackgroundResource(R.drawable.bg_input);
        }

        if (btnCheck != null) {
            btnCheck.setText("Check Answer");
            btnCheck.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#005AAE")));
            btnCheck.setOnClickListener(v -> checkAnswer());
        }

        if (btnHint != null) btnHint.setText("Show Hint");

        // Wire audio button: phát âm từ cần gõ
        if (btnPlayAudio != null) {
            String term     = card.getTerm();
            String audioUrl = card.getAudioUrl();
            btnPlayAudio.setOnClickListener(v -> playAudio(term, audioUrl));
        }
    }

    /** Ưu tiên URL audio từ backend, fallback sang TTS */
    private void playAudio(String term, String audioUrl) {
        if (audioUrl != null && !audioUrl.isEmpty()) {
            try {
                MediaPlayer mp = new MediaPlayer();
                mp.setDataSource(audioUrl);
                mp.setOnPreparedListener(MediaPlayer::start);
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.setOnErrorListener((m, what, extra) -> {
                    m.release();
                    speakWithTTS(term);
                    return true;
                });
                mp.prepareAsync();
            } catch (Exception e) {
                speakWithTTS(term);
            }
        } else {
            speakWithTTS(term);
        }
    }

    private void speakWithTTS(String term) {
        if (tts != null && term != null && !term.isEmpty()) {
            tts.speak(term, TextToSpeech.QUEUE_FLUSH, null, "tts_" + term);
        }
    }

    private void checkAnswer() {
        if (etAnswer == null) return;
        String input = etAnswer.getText().toString().trim();
        if (TextUtils.isEmpty(input)) {
            Toast.makeText(this, "Hãy nhập câu trả lời!", Toast.LENGTH_SHORT).show();
            return;
        }

        SessionCardResponse card = cards.get(currentIndex);
        String correct = card.getTerm() != null ? card.getTerm() : "";
        boolean isCorrect = input.equalsIgnoreCase(correct);

        // Visual feedback
        etAnswer.setEnabled(false);
        if (isCorrect) {
            etAnswer.setBackgroundResource(R.drawable.bg_success);
        } else {
            etAnswer.setBackgroundResource(R.drawable.bg_error);
            wrongTerms.add(correct);
            // Show correct answer
            if (btnCheck != null) {
                btnCheck.setText("✓ " + correct);
                btnCheck.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                btnCheck.setEnabled(false);
            }
        }

        // Submit to API
        AnswerRequest req = new AnswerRequest(
                card.getFlashcardId(), sessionId, null, isCorrect);
        apiService.submitAnswer(req).enqueue(new Callback<ApiResponse<Void>>() {
            @Override public void onResponse(Call<ApiResponse<Void>> c, Response<ApiResponse<Void>> r) {}
            @Override public void onFailure(Call<ApiResponse<Void>> c, Throwable t) {}
        });

        // Hide keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && etAnswer != null)
            imm.hideSoftInputFromWindow(etAnswer.getWindowToken(), 0);

        // Next after delay
        new Handler().postDelayed(() -> {
            if (btnCheck != null) {
                btnCheck.setText("Check Answer");
                btnCheck.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#005AAE")));
                btnCheck.setEnabled(true);
                btnCheck.setOnClickListener(v -> checkAnswer());
            }
            showQuestion(currentIndex + 1);
        }, isCorrect ? 700 : 1500);
    }

    private void showHint() {
        SessionCardResponse card = cards.get(currentIndex);
        String term = card.getTerm() != null ? card.getTerm() : "";
        hintRevealed = Math.min(hintRevealed + 1, term.length());

        // Build hint: show first N letters, rest as "_"
        StringBuilder hint = new StringBuilder();
        for (int i = 0; i < term.length(); i++) {
            hint.append(i < hintRevealed ? term.charAt(i) : "_");
            if (i < term.length() - 1) hint.append(" ");
        }
        if (btnHint != null) btnHint.setText(hint.toString());
        if (etAnswer != null) {
            etAnswer.setText(term.substring(0, hintRevealed));
            etAnswer.setSelection(etAnswer.getText().length());
        }
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
        intent.putExtra(SessionResultActivity.EXTRA_MODE, "Spelling");
        startActivity(intent);
        finish();
    }

    private void confirmExit() {
        new AlertDialog.Builder(this)
                .setTitle("Thoát?")
                .setPositiveButton("Thoát", (d, w) -> finish())
                .setNegativeButton("Tiếp tục", null).show();
    }

    private void showLoading(boolean loading) {
        if (progressBar != null)
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
