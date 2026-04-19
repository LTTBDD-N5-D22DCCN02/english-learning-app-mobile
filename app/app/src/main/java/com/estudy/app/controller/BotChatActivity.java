package com.estudy.app.controller;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.estudy.app.R;
import com.estudy.app.bot.BotConfig;
import com.estudy.app.bot.BotMessage;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BotChatActivity extends AppCompatActivity {

    private static final String TAG           = "BotChat";
    private static final String KEY_ERROR     = "error";
    private static final int    PERM_REQUEST = 101;
    private static final int    SAMPLE_RATE  = 16000;
    private static final int    CHUNK_FRAMES = 1024;

    private static final String SYSTEM_PROMPT =
        "You are a friendly English-speaking friend having a natural conversation with a Vietnamese person who is learning English.\n" +
        "Your personality: warm, casual, fun — like a real friend, not a teacher.\n" +
        "\n" +
        "About corrections:\n" +
        "- Do NOT correct every small mistake — that kills the natural flow.\n" +
        "- Only step in when the user says something seriously wrong in meaning or context " +
        "(e.g. using a very formal phrase in a casual chat, or a phrase that means something completely different).\n" +
        "- When you do correct, switch briefly to Vietnamese to explain naturally, " +
        "for example: 'À, câu đó nghe hơi trang trọng quá cho lúc này, bạn có thể nói \"Would you like to...?\" sẽ tự nhiên hơn.'\n" +
        "- After correcting, encourage the user to try saying it again.\n" +
        "- Once they retry (or if they move on), return to normal English conversation immediately.\n" +
        "\n" +
        "General rules:\n" +
        "- Keep replies to 1 sentence only — short, spoken, natural.\n" +
        "- Occasionally (not always) end with a quick follow-up question to keep the chat going.\n" +
        "- Plain text only — no markdown, no bullet points, no emoji.";

    // ── Views ─────────────────────────────────────────────────────────
    private RecyclerView      rvMessages;
    private Button            btnStartStop;
    private TextView          tvStatus;
    private BotMessageAdapter adapter;
    private final List<BotMessage> messages = new ArrayList<>();

    // ── Audio ─────────────────────────────────────────────────────────
    private AudioRecord audioRecord;
    private AudioTrack  audioTrack;
    private volatile boolean isActive    = false;
    private volatile boolean botSpeaking = false;

    // ── Network ───────────────────────────────────────────────────────
    private OkHttpClient     okClient;
    private WebSocket        sttWs;
    private ExecutorService  executor;

    // ── Gemini history ────────────────────────────────────────────────
    private final List<JSONObject> chatHistory = new ArrayList<>();

    // ── Partial transcript tracking ───────────────────────────────────
    private int partialMsgIndex = -1;

    // ─────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bot_chat);

        okClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
        executor = Executors.newCachedThreadPool();

        rvMessages   = findViewById(R.id.rvMessages);
        btnStartStop = findViewById(R.id.btnStartStop);
        tvStatus     = findViewById(R.id.tvStatus);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        rvMessages.setLayoutManager(llm);
        adapter = new BotMessageAdapter(messages);
        rvMessages.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnStartStop.setOnClickListener(v -> {
            if (!isActive) startSession();
            else stopSession();
        });
    }

    // ── Permission ─────────────────────────────────────────────────────
    private void startSession() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, PERM_REQUEST);
        } else {
            doStart();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] perms,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, perms, results);
        if (requestCode == PERM_REQUEST
                && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            doStart();
        } else {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show();
        }
    }

    // ── Start / Stop ───────────────────────────────────────────────────
    private void doStart() {
        isActive = true;
        botSpeaking = false;
        chatHistory.clear();
        messages.clear();
        partialMsgIndex = -1;
        adapter.notifyDataSetChanged();

        btnStartStop.setText("Stop");
        setStatus("Connecting…");

        setupAudioTrack();
        setupAudioRecord();  // mic ready BEFORE WebSocket connects
        startSendLoop();     // send loop runs for entire session
        connectSTT();
    }

    private void stopSession() {
        isActive    = false;
        botSpeaking = false;

        if (sttWs != null) {
            sttWs.close(1000, "User stopped");
            sttWs = null;
        }
        releaseAudioRecord();
        releaseAudioTrack();

        runOnUiThread(() -> {
            btnStartStop.setText("Start");
            setStatus("Ready");
        });
    }

    private void releaseAudioRecord() {
        if (audioRecord != null) {
            try { audioRecord.stop(); } catch (Exception ignored) {}
            audioRecord.release();
            audioRecord = null;
        }
    }

    private void releaseAudioTrack() {
        if (audioTrack != null) {
            try { audioTrack.stop(); } catch (Exception ignored) {}
            audioTrack.release();
            audioTrack = null;
        }
    }

    // ── AudioTrack (speaker) ───────────────────────────────────────────
    private void setupAudioTrack() {
        int minBuf = AudioTrack.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                minBuf, AudioTrack.MODE_STREAM);
        audioTrack.play();
    }

    // ── STT WebSocket ──────────────────────────────────────────────────
    private void connectSTT() {
        String url = "wss://api.elevenlabs.io/v1/speech-to-text/realtime"
                + "?model_id=scribe_v2_realtime&language_code=en&commit_strategy=vad";

        Request req = new Request.Builder()
                .url(url)
                .addHeader("xi-api-key", BotConfig.ELEVEN_API_KEY)
                .build();

        sttWs = okClient.newWebSocket(req, new WebSocketListener() {
            @Override public void onOpen(WebSocket ws, Response response) {
                setStatus("Listening…");
            }
            @Override public void onMessage(WebSocket ws, String text) {
                handleSTTMessage(text);
            }
            @Override public void onFailure(WebSocket ws, Throwable t, Response response) {
                Log.e(TAG, "STT failure: " + t.getMessage());
                sttWs = null;
                if (isActive && !botSpeaking) {
                    // Server closes after each committed_transcript — reconnect automatically
                    executor.execute(() -> {
                        try {
                            Thread.sleep(600);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        connectSTT();
                    });
                }
            }
            @Override public void onClosed(WebSocket ws, int code, String reason) {
                Log.d(TAG, "STT closed: " + code);
                sttWs = null;
                if (isActive && !botSpeaking) {
                    executor.execute(() -> {
                        try {
                            Thread.sleep(600);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        connectSTT();
                    });
                }
            }
        });
    }

    // ── AudioRecord setup (called before WebSocket connects) ─────────
    private void setupAudioRecord() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) return;

        int minBuf  = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int bufSize = Math.max(minBuf, CHUNK_FRAMES * 2);

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize);

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord failed to initialize");
            setStatus("Mic error");
            return;
        }
        audioRecord.startRecording();
        Log.d(TAG, "AudioRecord started");
    }

    // ── Send loop (started after WebSocket onOpen) ────────────────────
    private void startSendLoop() {
        final byte[] silence = new byte[CHUNK_FRAMES * 2];

        executor.execute(() -> {
            byte[] buf = new byte[CHUNK_FRAMES * 2];
            while (isActive) {
                AudioRecord ar = audioRecord;
                WebSocket   ws = sttWs;
                if (ar == null || ws == null) {
                    try { Thread.sleep(20); } catch (InterruptedException ignored) {}
                    continue;
                }

                int read = ar.read(buf, 0, buf.length);
                if (read <= 0) continue;

                byte[] payload = botSpeaking ? silence : buf;
                String b64 = Base64.encodeToString(payload, Base64.NO_WRAP);
                try {
                    JSONObject msg = new JSONObject();
                    msg.put("message_type", "input_audio_chunk");
                    msg.put("audio_base_64", b64);
                    msg.put("sample_rate", SAMPLE_RATE);
                    ws.send(msg.toString());
                } catch (JSONException e) {
                    Log.e(TAG, "JSON build error", e);
                }
            }
        });
    }

    // ── STT message handler ────────────────────────────────────────────
    private void handleSTTMessage(String raw) {
        Log.d(TAG, "WS← " + raw);
        try {
            JSONObject msg   = new JSONObject(raw);
            String     mtype = msg.optString("message_type");

            if ("session_started".equals(mtype)) {
                setStatus("Listening…");

            } else if ("partial_transcript".equals(mtype)) {
                if (botSpeaking) return;
                String partial = msg.optString("text", "");
                runOnUiThread(() -> updatePartialMessage(partial));

            } else if ("committed_transcript".equals(mtype)) {
                String userText = msg.optString("text", "").trim();
                if (userText.isEmpty() || botSpeaking) return;
                runOnUiThread(() -> finalizeUserMessage(userText));
                executor.execute(() -> respondToUser(userText));

            } else if ("quota_exceeded".equals(mtype)) {
                String err = msg.optString(KEY_ERROR, "ElevenLabs STT quota exceeded");
                Log.e(TAG, "quota_exceeded: " + err);
                isActive = false;
                setStatus("Quota exceeded");
                runOnUiThread(() -> {
                    btnStartStop.setText("Start");
                    Toast.makeText(BotChatActivity.this,
                            "ElevenLabs STT hết quota. Vui lòng tạo key mới.",
                            Toast.LENGTH_LONG).show();
                });

            } else if (KEY_ERROR.equals(mtype)) {
                String err = msg.optString(KEY_ERROR, "Unknown STT error");
                Log.e(TAG, "STT error msg: " + err);
                setStatus("Error: " + err);
                runOnUiThread(() -> Toast.makeText(BotChatActivity.this,
                        "STT error: " + err, Toast.LENGTH_LONG).show());
            }
        } catch (JSONException e) {
            Log.e(TAG, "STT parse error", e);
        }
    }

    // ── Partial transcript UI ──────────────────────────────────────────
    private void updatePartialMessage(String text) {
        if (text.isEmpty()) return;
        if (partialMsgIndex >= 0 && partialMsgIndex < messages.size()) {
            messages.get(partialMsgIndex).setText("🎤 " + text);
            adapter.notifyItemChanged(partialMsgIndex);
        } else {
            messages.add(new BotMessage("🎤 " + text, BotMessage.Type.USER));
            partialMsgIndex = messages.size() - 1;
            adapter.notifyItemInserted(partialMsgIndex);
            rvMessages.scrollToPosition(partialMsgIndex);
        }
    }

    private void finalizeUserMessage(String text) {
        if (partialMsgIndex >= 0 && partialMsgIndex < messages.size()) {
            messages.get(partialMsgIndex).setText(text);
            adapter.notifyItemChanged(partialMsgIndex);
        } else {
            messages.add(new BotMessage(text, BotMessage.Type.USER));
            adapter.notifyItemInserted(messages.size() - 1);
        }
        partialMsgIndex = -1;
        rvMessages.scrollToPosition(messages.size() - 1);
    }

    // ── Bot response pipeline ─────────────────────────────────────────
    private void respondToUser(String userText) {
        botSpeaking = true;
        setStatus("Thinking…");

        String botResponse = callGemini(userText);
        if (botResponse == null || botResponse.isEmpty()) {
            botSpeaking = false;
            setStatus("Listening…");
            return;
        }

        runOnUiThread(() -> {
            messages.add(new BotMessage(botResponse, BotMessage.Type.BOT));
            adapter.notifyItemInserted(messages.size() - 1);
            rvMessages.scrollToPosition(messages.size() - 1);
        });

        setStatus("Speaking…");
        playTTS(botResponse);

        try { Thread.sleep(600); } catch (InterruptedException ignored) {}
        botSpeaking = false;
        setStatus("Listening…");
    }

    // ── Gemini REST API ────────────────────────────────────────────────
    private String callGemini(String userText) {
        try {
            JSONObject userTurn = new JSONObject();
            userTurn.put("role", "user");
            JSONArray userParts = new JSONArray();
            userParts.put(new JSONObject().put("text", userText));
            userTurn.put("parts", userParts);
            chatHistory.add(userTurn);

            JSONObject sysInst = new JSONObject();
            JSONArray  sysParts = new JSONArray();
            sysParts.put(new JSONObject().put("text", SYSTEM_PROMPT));
            sysInst.put("parts", sysParts);

            JSONArray contents = new JSONArray();
            for (JSONObject turn : chatHistory) contents.put(turn);

            JSONObject body = new JSONObject();
            body.put("system_instruction", sysInst);
            body.put("contents", contents);

            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + BotConfig.GEMINI_MODEL + ":generateContent?key=" + BotConfig.GEMINI_API_KEY;

            RequestBody reqBody = RequestBody.create(
                    body.toString(), MediaType.parse("application/json; charset=utf-8"));

            Request request = new Request.Builder().url(url).post(reqBody).build();

            try (Response response = okClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    int code = response.code();
                    Log.e(TAG, "Gemini error " + code);
                    chatHistory.remove(chatHistory.size() - 1);
                    String errMsg;
                    if (code == 429) {
                        errMsg = "Gemini hết quota (429). Vào aistudio.google.com kiểm tra key.";
                    } else if (code == 400) {
                        errMsg = "Gemini API key không hợp lệ (400).";
                    } else {
                        errMsg = "Gemini lỗi " + code;
                    }
                    final String toastMsg = errMsg;
                    runOnUiThread(() -> Toast.makeText(BotChatActivity.this,
                            toastMsg, Toast.LENGTH_LONG).show());
                    setStatus(errMsg);
                    return null;
                }
                String json = response.body().string();
                String text = new JSONObject(json)
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                        .trim();

                JSONObject modelTurn = new JSONObject();
                modelTurn.put("role", "model");
                JSONArray modelParts = new JSONArray();
                modelParts.put(new JSONObject().put("text", text));
                modelTurn.put("parts", modelParts);
                chatHistory.add(modelTurn);

                return text;
            }
        } catch (Exception e) {
            Log.e(TAG, "Gemini call failed", e);
            return null;
        }
    }

    // ── ElevenLabs TTS ─────────────────────────────────────────────────
    private void playTTS(String text) {
        try {
            String url = "https://api.elevenlabs.io/v1/text-to-speech/"
                    + BotConfig.VOICE_ID + "?output_format=pcm_16000";

            JSONObject body = new JSONObject();
            body.put("text", text);
            body.put("model_id", BotConfig.TTS_MODEL);

            RequestBody reqBody = RequestBody.create(
                    body.toString(), MediaType.parse("application/json; charset=utf-8"));

            Request request = new Request.Builder()
                    .url(url)
                    .post(reqBody)
                    .addHeader("xi-api-key", BotConfig.ELEVEN_API_KEY)
                    .build();

            try (Response response = okClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "TTS error " + response.code());
                    return;
                }
                byte[] pcm = response.body().bytes();
                AudioTrack track = audioTrack;
                if (track != null && pcm.length > 0) {
                    track.write(pcm, 0, pcm.length);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "TTS failed", e);
        }
    }

    // ── Helper ─────────────────────────────────────────────────────────
    private void setStatus(String status) {
        runOnUiThread(() -> { if (tvStatus != null) tvStatus.setText(status); });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSession();
        executor.shutdownNow();
    }
}
