package com.estudy.app.controller;

import android.animation.*;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.*;
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

public class FlashcardStudyActivity extends AppCompatActivity {

    public static final String EXTRA_SET_ID   = "set_id";
    public static final String EXTRA_SET_NAME = "set_name";

    private TextView  tvProgress, tvCardFront, tvCardIPA, tvCardBack, tvExample, tvTapHint;
    private View      cardFlashcard;
    private Button    btnForgot, btnRemembered, btnPlayAudio;
    private ProgressBar progressBar;
    private View      layoutBack;

    private ApiService apiService;
    private String sessionId;
    private List<SessionCardResponse> cards = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isFlipped = false;
    private final List<String> wrongTerms = new ArrayList<>();

    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard_study);

        TokenManager tm = new TokenManager(this);
        apiService = ApiClient.getInstance(tm).create(ApiService.class);

        // Init Text-To-Speech (tiếng Anh Mỹ)
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
            }
        });

        bindViews();

        String setId   = getIntent().getStringExtra(EXTRA_SET_ID);
        String setName = getIntent().getStringExtra(EXTRA_SET_NAME);

        startSession(setId, setName);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    private void bindViews() {
        tvProgress   = findViewById(R.id.tvProgress);
        tvCardFront  = findViewById(R.id.tvCardFront);
        tvCardIPA    = findViewById(R.id.tvCardIPA);
        tvTapHint    = findViewById(R.id.tvTapHint);
        cardFlashcard = findViewById(R.id.cardFlashcard);
        progressBar  = findViewById(R.id.progressBar);
        btnForgot     = findViewById(R.id.btnForgot);
        btnRemembered = findViewById(R.id.btnRemembered);
        btnPlayAudio  = findViewById(R.id.btnPlayAudio);

        // Back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> confirmExit());

        ImageButton btnExit = findViewById(R.id.btnExit);
        if (btnExit != null) btnExit.setOnClickListener(v -> confirmExit());

        // Initially hide action buttons
        btnForgot.setVisibility(View.GONE);
        btnRemembered.setVisibility(View.GONE);

        // Tap card to flip
        cardFlashcard.setOnClickListener(v -> flipCard());

        btnForgot.setOnClickListener(v    -> submitAnswer(false));
        btnRemembered.setOnClickListener(v -> submitAnswer(true));
    }

    private void startSession(String setId, String setName) {
        showLoading(true);
        apiService.startSession(new StartSessionRequest(setId, "flashcard"))
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
                        Toast.makeText(FlashcardStudyActivity.this,
                                "Không có thẻ nào để học!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        showCard(0);
                    }
                } else {
                    Toast.makeText(FlashcardStudyActivity.this,
                            "Không thể bắt đầu phiên học", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<StartSessionResponse>> call, Throwable t) {
                showLoading(false);
                Toast.makeText(FlashcardStudyActivity.this,
                        "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void showCard(int index) {
        if (index >= cards.size()) {
            endSession();
            return;
        }
        currentIndex = index;
        isFlipped    = false;

        SessionCardResponse card = cards.get(index);
        int total = cards.size();

        tvProgress.setText((index + 1) + " / " + total);
        progressBar.setProgress((index + 1) * 100 / total);

        // Front: term + IPA
        tvCardFront.setText(card.getTerm());
        if (tvCardIPA != null)
            tvCardIPA.setText(card.getIpa() != null ? card.getIpa() : "");
        if (tvTapHint != null) tvTapHint.setVisibility(View.VISIBLE);

        // Hide back side
        if (layoutBack != null) layoutBack.setVisibility(View.GONE);

        // Hide buttons until flipped
        btnForgot.setVisibility(View.GONE);
        btnRemembered.setVisibility(View.GONE);

        // Reset card rotation
        cardFlashcard.setRotationY(0f);

        // Wire audio button for this card
        if (btnPlayAudio != null) {
            String term     = card.getTerm();
            String audioUrl = card.getAudioUrl();
            btnPlayAudio.setOnClickListener(v -> playAudio(term, audioUrl));
        }
    }

    /** Phát âm từ: ưu tiên URL audio từ backend, fallback sang Android TTS */
    private void playAudio(String term, String audioUrl) {
        if (audioUrl != null && !audioUrl.isEmpty()) {
            try {
                MediaPlayer mp = new MediaPlayer();
                mp.setDataSource(audioUrl);
                mp.setOnPreparedListener(MediaPlayer::start);
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.setOnErrorListener((m, what, extra) -> {
                    m.release();
                    speakWithTTS(term); // fallback nếu URL lỗi
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

    private void flipCard() {
        if (isFlipped) return;
        isFlipped = true;

        SessionCardResponse card = cards.get(currentIndex);

        // Flip animation
        ObjectAnimator flipOut = ObjectAnimator.ofFloat(cardFlashcard, "rotationY", 0f, 90f);
        flipOut.setDuration(200);
        flipOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Show back content
                tvCardFront.setText(card.getDefinition() != null ? card.getDefinition() : "");
                if (tvCardIPA != null) tvCardIPA.setText("");
                if (tvTapHint != null) tvTapHint.setVisibility(View.GONE);
                if (layoutBack != null) {
                    layoutBack.setVisibility(View.VISIBLE);
                    if (tvCardBack != null)
                        tvCardBack.setText(card.getDefinition() != null ? card.getDefinition() : "");
                    if (tvExample != null && card.getExample() != null)
                        tvExample.setText(card.getExample());
                }

                ObjectAnimator flipIn = ObjectAnimator.ofFloat(cardFlashcard, "rotationY", -90f, 0f);
                flipIn.setDuration(200);
                flipIn.start();

                // Show action buttons
                btnForgot.setVisibility(View.VISIBLE);
                btnRemembered.setVisibility(View.VISIBLE);
            }
        });
        flipOut.start();
    }

    private void submitAnswer(boolean remembered) {
        SessionCardResponse card = cards.get(currentIndex);

        if (!remembered) wrongTerms.add(card.getTerm());

        AnswerRequest req = new AnswerRequest(
                card.getFlashcardId(), sessionId, remembered, null);

        apiService.submitAnswer(req).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call,
                                   Response<ApiResponse<Void>> response) {
                showCard(currentIndex + 1);
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                // Continue even on failure
                showCard(currentIndex + 1);
            }
        });
    }

    private void endSession() {
        if (sessionId == null) { finish(); return; }

        apiService.endSession(sessionId, wrongTerms)
                .enqueue(new Callback<ApiResponse<SessionResultResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<SessionResultResponse>> call,
                                   Response<ApiResponse<SessionResultResponse>> response) {
                SessionResultResponse result = null;
                if (response.isSuccessful() && response.body() != null)
                    result = response.body().getResult();

                Intent intent = new Intent(FlashcardStudyActivity.this,
                        SessionResultActivity.class);
                if (result != null) {
                    intent.putExtra(SessionResultActivity.EXTRA_CORRECT,  result.getCorrectCount());
                    intent.putExtra(SessionResultActivity.EXTRA_TOTAL,    result.getTotalQuestions());
                    intent.putExtra(SessionResultActivity.EXTRA_STREAK,   result.getCurrentStreak());
                    intent.putExtra(SessionResultActivity.EXTRA_NEW_RECORD, result.isNewRecord());
                    intent.putExtra(SessionResultActivity.EXTRA_DURATION, result.getDurationSeconds());
                    intent.putExtra(SessionResultActivity.EXTRA_MODE, "Flashcard");
                } else {
                    intent.putExtra(SessionResultActivity.EXTRA_CORRECT,  cards.size() - wrongTerms.size());
                    intent.putExtra(SessionResultActivity.EXTRA_TOTAL,    cards.size());
                    intent.putExtra(SessionResultActivity.EXTRA_MODE, "Flashcard");
                }
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Call<ApiResponse<SessionResultResponse>> call, Throwable t) {
                Intent intent = new Intent(FlashcardStudyActivity.this,
                        SessionResultActivity.class);
                intent.putExtra(SessionResultActivity.EXTRA_CORRECT, cards.size() - wrongTerms.size());
                intent.putExtra(SessionResultActivity.EXTRA_TOTAL,   cards.size());
                intent.putExtra(SessionResultActivity.EXTRA_MODE, "Flashcard");
                startActivity(intent);
                finish();
            }
        });
    }

    private void confirmExit() {
        new AlertDialog.Builder(this)
                .setTitle("Thoát khỏi phiên học?")
                .setMessage("Tiến trình sẽ không được lưu.")
                .setPositiveButton("Thoát", (d, w) -> finish())
                .setNegativeButton("Tiếp tục", null)
                .show();
    }

    private void showLoading(boolean loading) {
        if (progressBar != null)
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
