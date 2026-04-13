package com.estudy.app.controller;

import android.animation.*;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

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

import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public class FlashcardStudyActivity extends AppCompatActivity {

    public static final String EXTRA_SET_ID   = "set_id";
    public static final String EXTRA_SET_NAME = "set_name";

    private TextView    tvProgress;
    private ProgressBar progressBar;
    private TextView    tvForgotBadge, tvRememberedBadge;

    private View        cardFlashcard;
    private View        layoutFront, layoutBack;
    private TextView    tvCardFront, tvCardIPA, tvTapHint;
    private TextView    tvCardBackTerm, tvCardBackIPA, tvCardBack, tvExample;
    private ImageView   ivCardImage;
    private ImageButton btnPlayAudio, btnPlayAudioBack;

    private View overlayForgot, overlayRemembered;

    private MaterialButton btnForgot, btnRemembered, btnViewDetails;

    private ApiService apiService;
    private String sessionId;
    private List<SessionCardResponse> cards = new ArrayList<>();
    private int currentIndex    = 0;
    private boolean isFlipped   = false;
    private int forgotCount     = 0;
    private int rememberedCount = 0;
    private final List<String> wrongTerms = new ArrayList<>();

    private TextToSpeech tts;

    // Swipe tracking
    private float swipeStartX = 0f;
    private float swipeStartY = 0f;
    private boolean isSwiping = false;          // true khi đã xác định là swipe ngang
    private static final int SWIPE_COMMIT  = 160; // px để commit swipe
    private static final int SWIPE_DETECT  = 12;  // px nhỏ nhất để xác định hướng
    private static final float MAX_TILT    = 18f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard_study);

        TokenManager tm = new TokenManager(this);
        apiService = ApiClient.getInstance(tm).create(ApiService.class);

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
        tvProgress        = findViewById(R.id.tvProgress);
        progressBar       = findViewById(R.id.progressBar);
        tvForgotBadge     = findViewById(R.id.tvForgotBadge);
        tvRememberedBadge = findViewById(R.id.tvRememberedBadge);

        cardFlashcard  = findViewById(R.id.cardFlashcard);
        layoutFront    = findViewById(R.id.layoutFront);
        layoutBack     = findViewById(R.id.layoutBack);
        tvCardFront    = findViewById(R.id.tvCardFront);
        tvCardIPA      = findViewById(R.id.tvCardIPA);
        tvTapHint      = findViewById(R.id.tvTapHint);
        ivCardImage    = findViewById(R.id.ivCardImage);
        tvCardBackTerm = findViewById(R.id.tvCardBackTerm);
        tvCardBackIPA  = findViewById(R.id.tvCardBackIPA);
        tvCardBack     = findViewById(R.id.tvCardBack);
        tvExample      = findViewById(R.id.tvExample);
        btnPlayAudio      = findViewById(R.id.btnPlayAudio);
        btnPlayAudioBack  = findViewById(R.id.btnPlayAudioBack);
        overlayForgot     = findViewById(R.id.overlayForgot);
        overlayRemembered = findViewById(R.id.overlayRemembered);

        btnForgot      = findViewById(R.id.btnForgot);
        btnRemembered  = findViewById(R.id.btnRemembered);
        btnViewDetails = findViewById(R.id.btnViewDetails);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> confirmExit());
        ImageButton btnSettings = findViewById(R.id.btnSettings);
        if (btnSettings != null) btnSettings.setOnClickListener(v -> showSettingsSheet());

        // ── Touch handler: phân biệt tap (flip) vs swipe (submit) ──
        cardFlashcard.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    swipeStartX = event.getRawX();
                    swipeStartY = event.getRawY();
                    isSwiping   = false;
                    // QUAN TRỌNG: return false để click event vẫn hoạt động
                    return false;

                case MotionEvent.ACTION_MOVE: {
                    float dx = event.getRawX() - swipeStartX;
                    float dy = event.getRawY() - swipeStartY;

                    if (!isSwiping) {
                        // Chưa xác định hướng — chờ đủ ngưỡng
                        if (Math.abs(dx) < SWIPE_DETECT && Math.abs(dy) < SWIPE_DETECT)
                            return false; // chưa đủ → không can thiệp

                        if (Math.abs(dx) >= Math.abs(dy) * 1.5f) {
                            // Ngang rõ ràng → bắt đầu swipe mode
                            isSwiping = true;
                        } else {
                            // Dọc → không phải swipe ngang, bỏ qua
                            return false;
                        }
                    }

                    // Đang swipe ngang — áp dụng tilt + translate
                    float tilt = Math.max(-MAX_TILT, Math.min(MAX_TILT, (dx / 500f) * MAX_TILT));
                    cardFlashcard.setRotation(tilt);
                    cardFlashcard.setTranslationX(dx * 0.35f);

                    float alpha = Math.min(1f, Math.abs(dx) / 300f);
                    if (dx > 0) {
                        if (overlayRemembered != null) overlayRemembered.setAlpha(alpha);
                        if (overlayForgot != null)     overlayForgot.setAlpha(0f);
                    } else {
                        if (overlayForgot != null)     overlayForgot.setAlpha(alpha);
                        if (overlayRemembered != null) overlayRemembered.setAlpha(0f);
                    }
                    // return true chỉ khi đang swipe, để ngăn scroll cha
                    return true;
                }

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    if (!isSwiping) {
                        // Đây là TAP — để click listener xử lý, không làm gì
                        isSwiping = false;
                        return false;
                    }
                    // Đây là SWIPE
                    isSwiping = false;
                    float finalDx = event.getRawX() - swipeStartX;

                    if (Math.abs(finalDx) >= SWIPE_COMMIT) {
                        animateFlyAway(finalDx > 0);
                    } else {
                        snapBack();
                    }
                    return true;
                }
            }
            return false;
        });

        // Click để flip — hoạt động bình thường vì touch chỉ return true khi swipe
        cardFlashcard.setOnClickListener(v -> flipCard());

        if (btnForgot != null)     btnForgot.setOnClickListener(v -> animateFlyAway(false));
        if (btnRemembered != null) btnRemembered.setOnClickListener(v -> animateFlyAway(true));
        if (btnViewDetails != null) btnViewDetails.setOnClickListener(v -> showCardDetails());
    }

    // ── Snap back ─────────────────────────────────────────────
    private void snapBack() {
        cardFlashcard.animate().rotation(0f).translationX(0f).setDuration(200).start();
        if (overlayForgot != null)     overlayForgot.setAlpha(0f);
        if (overlayRemembered != null) overlayRemembered.setAlpha(0f);
    }

    // ── Fly away ──────────────────────────────────────────────
    private void animateFlyAway(boolean remembered) {
        if (overlayForgot != null)     overlayForgot.setAlpha(0f);
        if (overlayRemembered != null) overlayRemembered.setAlpha(0f);

        float targetX   = remembered ? 1200f : -1200f;
        float targetRot = remembered ? 30f : -30f;

        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(cardFlashcard, "translationX",
                        cardFlashcard.getTranslationX(), targetX),
                ObjectAnimator.ofFloat(cardFlashcard, "rotation",
                        cardFlashcard.getRotation(), targetRot),
                ObjectAnimator.ofFloat(cardFlashcard, "alpha", 1f, 0f)
        );
        set.setDuration(300);
        set.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                cardFlashcard.setTranslationX(0f);
                cardFlashcard.setRotation(0f);
                cardFlashcard.setAlpha(1f);
                submitAnswer(remembered);
            }
        });
        set.start();
    }

    // ── Flip (tap) ─────────────────────────────────────────────
    private void flipCard() {
        flipTo(!isFlipped);
    }

    private void flipTo(boolean toBack) {
        // Animation: rotationY (3D flip) — KHÔNG dùng rotation (2D tilt của swipe)
        ObjectAnimator out = ObjectAnimator.ofFloat(cardFlashcard, "rotationY", 0f, 90f);
        out.setDuration(150);
        out.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                if (toBack) {
                    layoutFront.setVisibility(View.GONE);
                    layoutBack.setVisibility(View.VISIBLE);
                    SessionCardResponse c = cards.get(currentIndex);
                    if (tvCardBackTerm != null)
                        tvCardBackTerm.setText(c.getTerm() != null ? c.getTerm() : "");
                    if (tvCardBackIPA != null)
                        tvCardBackIPA.setText(c.getIpa() != null ? c.getIpa() : "");
                    if (tvCardBack != null)
                        tvCardBack.setText(c.getDefinition() != null ? c.getDefinition() : "");
                    if (tvExample != null)
                        tvExample.setText(c.getExample() != null ? c.getExample() : "");
                } else {
                    layoutBack.setVisibility(View.GONE);
                    layoutFront.setVisibility(View.VISIBLE);
                    if (tvTapHint != null) tvTapHint.setVisibility(View.VISIBLE);
                }
                ObjectAnimator in = ObjectAnimator.ofFloat(cardFlashcard, "rotationY", -90f, 0f);
                in.setDuration(150);
                in.start();
            }
        });
        out.start();
        isFlipped = toBack;
    }

    // ── Session ───────────────────────────────────────────────
    private void startSession(String setId) {
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
                                updateBadges();
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
        if (index >= cards.size()) { endSession(); return; }
        currentIndex = index;
        isFlipped    = false;

        SessionCardResponse card = cards.get(index);
        int total = cards.size();

        tvProgress.setText(String.format(Locale.getDefault(), "%d / %d", index + 1, total));
        progressBar.setProgress((index + 1) * 100 / total);

        tvCardFront.setText(card.getTerm() != null ? card.getTerm() : "");
        if (tvCardIPA != null) tvCardIPA.setText(card.getIpa() != null ? card.getIpa() : "");

        if (ivCardImage != null) {
            String imageData = card.getImage();
            boolean hasImage = imageData != null && !imageData.isEmpty();
            if (hasImage) {
                ivCardImage.setVisibility(View.VISIBLE);
                if (imageData.startsWith("data:image")) {
                    // Base64
                    try {
                        String b64 = imageData.substring(imageData.indexOf(",") + 1);
                        byte[] bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
                        Glide.with(this).load(bytes)
                                .centerCrop()
                                .into(ivCardImage);
                    } catch (Exception e) {
                        ivCardImage.setVisibility(View.GONE);
                    }
                } else {
                    // URL thường
                    Glide.with(this).load(imageData)
                            .centerCrop()
                            .into(ivCardImage);
                }
            } else {
                ivCardImage.setVisibility(View.GONE);
            }
        }

        layoutFront.setVisibility(View.VISIBLE);
        layoutBack.setVisibility(View.GONE);
        if (tvTapHint != null) tvTapHint.setVisibility(View.VISIBLE);

        // Reset rotations khi load thẻ mới
        cardFlashcard.setRotationY(0f);
        cardFlashcard.setRotation(0f);
        cardFlashcard.setTranslationX(0f);
        cardFlashcard.setAlpha(1f);

        View.OnClickListener audioL = v -> playAudio(card.getTerm(), card.getAudioUrl());
        if (btnPlayAudio != null)     btnPlayAudio.setOnClickListener(audioL);
        if (btnPlayAudioBack != null) btnPlayAudioBack.setOnClickListener(audioL);
    }

    // ── Submit ────────────────────────────────────────────────
    private void submitAnswer(boolean remembered) {
        if (currentIndex >= cards.size()) return;
        SessionCardResponse card = cards.get(currentIndex);

        if (remembered) rememberedCount++;
        else { forgotCount++; wrongTerms.add(card.getTerm()); }
        updateBadges();

        AnswerRequest req = new AnswerRequest(card.getFlashcardId(), sessionId, remembered, null);
        apiService.submitAnswer(req).enqueue(new Callback<ApiResponse<Void>>() {
            @Override public void onResponse(Call<ApiResponse<Void>> c, Response<ApiResponse<Void>> r) {
                showCard(currentIndex + 1);
            }
            @Override public void onFailure(Call<ApiResponse<Void>> c, Throwable t) {
                showCard(currentIndex + 1);
            }
        });
    }

    private void updateBadges() {
        int total = cards.size();
        if (tvForgotBadge != null)
            tvForgotBadge.setText(String.format(Locale.getDefault(), "%d/%d", forgotCount, total));
        if (tvRememberedBadge != null)
            tvRememberedBadge.setText(String.format(Locale.getDefault(), "%d/%d", rememberedCount, total));
    }

    // ── Settings sheet ────────────────────────────────────────
    private void showSettingsSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.fragment_flashcard_settings_sheet, null);
        sheet.setContentView(v);
        ImageButton btnClose = v.findViewById(R.id.btnCloseSettings);
        if (btnClose != null) btnClose.setOnClickListener(x -> sheet.dismiss());
        TextView btnExit = v.findViewById(R.id.btnExitSession);
        if (btnExit != null) btnExit.setOnClickListener(x -> { sheet.dismiss(); confirmExit(); });
        sheet.show();
    }

    // ── Audio ─────────────────────────────────────────────────
    private void playAudio(String term, String audioUrl) {
        if (audioUrl != null && !audioUrl.isEmpty()) {
            try {
                MediaPlayer mp = new MediaPlayer();
                mp.setDataSource(audioUrl);
                mp.setOnPreparedListener(MediaPlayer::start);
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.setOnErrorListener((m, w, e) -> { m.release(); speakWithTTS(term); return true; });
                mp.prepareAsync();
                return;
            } catch (Exception ignored) {}
        }
        speakWithTTS(term);
    }

    private void speakWithTTS(String term) {
        if (tts != null && !TextUtils.isEmpty(term))
            tts.speak(term, TextToSpeech.QUEUE_FLUSH, null, "tts_" + term);
    }

    // ── Card details ──────────────────────────────────────────
    private void showCardDetails() {
        if (cards.isEmpty() || currentIndex >= cards.size()) return;
        SessionCardResponse c = cards.get(currentIndex);
        String msg = "Term: " + c.getTerm()
                + "\nIPA: " + (c.getIpa() != null ? c.getIpa() : "—")
                + "\nDefinition: " + (c.getDefinition() != null ? c.getDefinition() : "—")
                + "\nExample: " + (c.getExample() != null ? c.getExample() : "—");
        new AlertDialog.Builder(this).setTitle(c.getTerm())
                .setMessage(msg).setPositiveButton("OK", null).show();
    }

    // ── End session ───────────────────────────────────────────
    private void endSession() {
        if (sessionId == null) { finish(); return; }
        apiService.endSession(sessionId, wrongTerms)
                .enqueue(new Callback<ApiResponse<SessionResultResponse>>() {
                    @Override public void onResponse(Call<ApiResponse<SessionResultResponse>> c,
                                                     Response<ApiResponse<SessionResultResponse>> r) {
                        goToResult(r.isSuccessful() && r.body() != null ? r.body().getResult() : null);
                    }
                    @Override public void onFailure(Call<ApiResponse<SessionResultResponse>> c, Throwable t) {
                        goToResult(null);
                    }
                });
    }

    private void goToResult(SessionResultResponse result) {
        Intent i = new Intent(this, SessionResultActivity.class);
        if (result != null) {
            i.putExtra(SessionResultActivity.EXTRA_CORRECT,    result.getCorrectCount());
            i.putExtra(SessionResultActivity.EXTRA_TOTAL,      result.getTotalQuestions());
            i.putExtra(SessionResultActivity.EXTRA_STREAK,     result.getCurrentStreak());
            i.putExtra(SessionResultActivity.EXTRA_NEW_RECORD, result.isNewRecord());
            i.putExtra(SessionResultActivity.EXTRA_DURATION,   result.getDurationSeconds());
        } else {
            i.putExtra(SessionResultActivity.EXTRA_CORRECT, rememberedCount);
            i.putExtra(SessionResultActivity.EXTRA_TOTAL,   cards.size());
        }
        i.putExtra(SessionResultActivity.EXTRA_MODE, "Flashcard");
        startActivity(i);
        finish();
    }

    // ── Confirm exit ──────────────────────────────────────────
    private void confirmExit() {
        new AlertDialog.Builder(this)
                .setTitle("Thoát khỏi phiên học?")
                .setMessage("Kết quả hiện tại sẽ được lưu lại.")
                .setPositiveButton("Thoát", (d, w) -> {
                    if (sessionId != null) {
                        apiService.endSession(sessionId, wrongTerms).enqueue(
                                new Callback<ApiResponse<SessionResultResponse>>() {
                                    @Override public void onResponse(Call<ApiResponse<SessionResultResponse>> c,
                                                                     Response<ApiResponse<SessionResultResponse>> r) { finish(); }
                                    @Override public void onFailure(Call<ApiResponse<SessionResultResponse>> c, Throwable t) { finish(); }
                                });
                    } else finish();
                })
                .setNegativeButton("Tiếp tục", null).show();
    }

    private void showLoading(boolean loading) {
        if (progressBar != null)
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}