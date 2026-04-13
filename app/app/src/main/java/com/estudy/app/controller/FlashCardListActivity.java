package com.estudy.app.controller;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.estudy.app.R;
import com.estudy.app.api.ApiClient;
import com.estudy.app.api.ApiService;
import com.estudy.app.model.request.FlashCardRequest;
import com.estudy.app.model.response.ApiResponse;
import com.estudy.app.model.response.FlashCardResponse;
import com.estudy.app.model.response.FlashCardSetDetailResponse;
import com.estudy.app.model.response.SuggestResponse;
import com.estudy.app.utils.BottomNavHelper;
import com.estudy.app.utils.TokenManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import retrofit2.Call;
import retrofit2.Callback;

public class FlashCardListActivity extends AppCompatActivity
        implements FlashCardAdapter.Listener {

    public static final String EXTRA_SET_ID    = "flashcard_set_id";
    public static final String EXTRA_EDIT_MODE = "is_edit_mode";
    public static final String EXTRA_SET_NAME  = "flashcard_set_name";

    private static final String PIXABAY_KEY = "55417267-2e8894cb4aa79dda868d6e654";

    private RecyclerView rvFlashCards;
    private TextView     tvTitle;
    private View         layoutBottomActions;
    private View         layoutViewActions;

    private ApiService    apiService;
    private OkHttpClient  okHttpClient;   // FIX DNS: dùng OkHttp thay HttpURLConnection
    private String        flashCardSetId;
    private String        flashCardSetName;
    private boolean       isEditMode;

    private FlashCardAdapter adapter;
    private final List<FlashCardAdapter.CardData> cardList = new ArrayList<>();

    private int     pendingImagePosition = -1;
    private boolean suppressNextResume   = false;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                suppressNextResume = true;
                if (result.getResultCode() == Activity.RESULT_OK
                        && result.getData() != null && pendingImagePosition >= 0) {
                    String b64 = uriToBase64(result.getData().getData());
                    if (b64 != null) adapter.updateCardImage(pendingImagePosition, b64);
                    else toast("Loi khi doc anh!");
                }
                pendingImagePosition = -1;
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard_list);
        BottomNavHelper.setup(this, R.id.btnNavSets);

        // FIX DNS: OkHttpClient khởi tạo 1 lần, dùng cho Pixabay
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();

        TokenManager tokenManager = new TokenManager(this);
        apiService = ApiClient.getInstance(tokenManager).create(ApiService.class);

        flashCardSetId   = getIntent().getStringExtra(EXTRA_SET_ID);
        flashCardSetName = getIntent().getStringExtra(EXTRA_SET_NAME);
        isEditMode       = getIntent().getBooleanExtra(EXTRA_EDIT_MODE, false);

        View toolbar        = findViewById(R.id.toolbar);
        ImageButton btnBack = toolbar.findViewById(R.id.btnBack);
        tvTitle             = toolbar.findViewById(R.id.tvToolbarTitle);
        tvTitle.setText(flashCardSetName != null ? flashCardSetName
                : (isEditMode ? "Edit Flashcards" : "Loading..."));
        btnBack.setOnClickListener(v -> finish());

        View btnToolbarSave = toolbar.findViewById(R.id.btnSave);
        if (btnToolbarSave != null) {
            btnToolbarSave.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
            btnToolbarSave.setOnClickListener(v -> saveAllUnsaved());
        }

        layoutViewActions = findViewById(R.id.layoutViewActions);
        if (!isEditMode) {
            layoutViewActions.setVisibility(View.VISIBLE);
            findViewById(R.id.btnGoToEdit).setOnClickListener(v -> {
                Intent i = new Intent(this, FlashCardListActivity.class);
                i.putExtra(EXTRA_SET_ID,    flashCardSetId);
                i.putExtra(EXTRA_SET_NAME,  flashCardSetName);
                i.putExtra(EXTRA_EDIT_MODE, true);
                startActivity(i);
            });
        } else {
            layoutViewActions.setVisibility(View.GONE);
        }

        rvFlashCards = findViewById(R.id.rvFlashCards);
        rvFlashCards.setHasFixedSize(false);
        rvFlashCards.setNestedScrollingEnabled(true);
        rvFlashCards.setLayoutManager(new LinearLayoutManager(this));

        if (isEditMode) {
            adapter = new FlashCardAdapter(this, cardList, this, true);
            rvFlashCards.setAdapter(adapter);
        }

        layoutBottomActions = findViewById(R.id.layoutBottomActions);
        if (isEditMode && layoutBottomActions != null) {
            layoutBottomActions.setVisibility(View.VISIBLE);
            findViewById(R.id.btnAddCard).setOnClickListener(v -> {
                adapter.addNewCard();
                rvFlashCards.post(() ->
                        rvFlashCards.smoothScrollToPosition(adapter.getItemCount() - 1));
            });
            findViewById(R.id.btnImport).setOnClickListener(v -> {
                suppressNextResume = true;
                Intent i = new Intent(this, FlashCardImportActivity.class);
                i.putExtra(FlashCardImportActivity.EXTRA_SET_ID, flashCardSetId);
                startActivity(i);
            });
        }

        loadFlashCards();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isEditMode && suppressNextResume) {
            suppressNextResume = false;
            return;
        }
        if (isEditMode) loadFlashCards();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void loadFlashCards() {
        apiService.getFlashCardSetDetail(flashCardSetId)
                .enqueue(new Callback<ApiResponse<FlashCardSetDetailResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<FlashCardSetDetailResponse>> call,
                                           retrofit2.Response<ApiResponse<FlashCardSetDetailResponse>> response) {
                        if (!response.isSuccessful() || response.body() == null
                                || response.body().getResult() == null) return;
                        FlashCardSetDetailResponse detail = response.body().getResult();
                        if (detail.getName() != null) {
                            flashCardSetName = detail.getName();
                            tvTitle.setText(flashCardSetName);
                        }
                        List<FlashCardResponse> serverCards =
                                detail.getFlashCards() != null ? detail.getFlashCards() : new ArrayList<>();
                        if (isEditMode) {
                            cardList.clear();
                            for (FlashCardResponse r : serverCards)
                                cardList.add(new FlashCardAdapter.CardData(r));
                            adapter.notifyDataSetChanged();
                        } else {
                            rvFlashCards.setAdapter(new FlashCardAdapter(FlashCardListActivity.this, serverCards));                        }
                    }
                    @Override
                    public void onFailure(Call<ApiResponse<FlashCardSetDetailResponse>> call, Throwable t) {
                        toast("Load failed: " + t.getMessage());
                    }
                });
    }

    @Override
    public void onSave(FlashCardAdapter.CardData card, int position) {
        FlashCardRequest req = new FlashCardRequest(
                card.term, card.definition, card.ipa, card.example, card.imageUrl);
        if (card.isNew || card.id == null) {
            apiService.createFlashCard(flashCardSetId, req)
                    .enqueue(new Callback<ApiResponse<FlashCardResponse>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<FlashCardResponse>> call,
                                               retrofit2.Response<ApiResponse<FlashCardResponse>> response) {
                            if (response.isSuccessful() && response.body() != null
                                    && response.body().getResult() != null) {
                                adapter.markSaved(position, response.body().getResult().getId());
                                setResult(RESULT_OK);
                                toast("Saved!");
                            } else showApiError(response);
                        }
                        @Override
                        public void onFailure(Call<ApiResponse<FlashCardResponse>> call, Throwable t) { toast("Network error"); }
                    });
        } else {
            apiService.updateFlashCard(card.id, req)
                    .enqueue(new Callback<ApiResponse<FlashCardResponse>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<FlashCardResponse>> call,
                                               retrofit2.Response<ApiResponse<FlashCardResponse>> response) {
                            if (response.isSuccessful()) { setResult(RESULT_OK); toast("Updated!"); }
                            else showApiError(response);
                        }
                        @Override
                        public void onFailure(Call<ApiResponse<FlashCardResponse>> call, Throwable t) { toast("Network error"); }
                    });
        }
    }

    @Override
    public void onDelete(FlashCardAdapter.CardData card, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete flashcard")
                .setMessage(TextUtils.isEmpty(card.term) ? "Delete this card?" : "Delete \"" + card.term + "\"?")
                .setPositiveButton("Delete", (d, w) -> {
                    if (card.isNew || card.id == null) {
                        adapter.removeCard(position);
                    } else {
                        apiService.deleteFlashCard(card.id).enqueue(new Callback<ApiResponse<Void>>() {
                            @Override
                            public void onResponse(Call<ApiResponse<Void>> call, retrofit2.Response<ApiResponse<Void>> r) {
                                if (r.isSuccessful()) { adapter.removeCard(position); setResult(RESULT_OK); toast("Deleted"); }
                                else toast("Delete failed");
                            }
                            @Override
                            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) { toast("Network error"); }
                        });
                    }
                })
                .setNegativeButton("Cancel", null).show();
    }

    @Override
    public void onSuggest(int position, String term, FlashCardAdapter.SuggestCallback cb) {
        toast("Đang dịch từ: " + term + "...");

        apiService.suggest(term).enqueue(new Callback<ApiResponse<SuggestResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<SuggestResponse>> call,
                                   retrofit2.Response<ApiResponse<SuggestResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                    SuggestResponse s = response.body().getResult();
                    cb.onResult(nvl(s.getDefinition()), nvl(s.getIpa()), nvl(s.getExample()));
                    toast("Đã điền xong!");
                } else {
                    // ĐỌC CHÍNH XÁC LỖI TỪ SPRING BOOT
                    String errorMsg = "Lỗi HTTP " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " - " + response.errorBody().string();
                        }
                    } catch (Exception ignored) {}

                    toast(errorMsg); // Bắn Toast báo lỗi lên màn hình
                    android.util.Log.e("LOI_SUGGEST", "Chi tiết lỗi: " + errorMsg);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<SuggestResponse>> call, Throwable t) {
                toast("Lỗi kết nối Server: " + t.getMessage());
                android.util.Log.e("LOI_SUGGEST", "Lỗi mạng: " + t.getMessage());
            }
        });
    }

    @Override
    public void onImagePick(int position) {
        pendingImagePosition = position;
        imagePickerLauncher.launch(
                new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
    }

    /**
     * FIX DNS: Dùng OkHttp (đã có sẵn qua Retrofit) thay HttpURLConnection.
     * OkHttp có DNS fallback tốt hơn, resolve được pixabay.com trên emulator.
     */
    @Override
    public void onImageSuggest(int position, String term) {
        toast("Searching images for \"" + term + "\"...");
        executor.execute(() -> {
            List<String> urls = fetchPixabay(term);
            if (urls == null || urls.isEmpty()) {
                runOnUiThread(() -> toast("No image found for \"" + term + "\""));
                return;
            }
            runOnUiThread(() -> adapter.setSuggestedImages(position, urls));
        });
    }

    private List<String> fetchPixabay(String term) {
        try {
            String encodedTerm = URLEncoder.encode(term, "UTF-8");
            String url = "https://pixabay.com/api/?key=" + PIXABAY_KEY
                    + "&q=" + encodedTerm
                    + "&image_type=photo&per_page=10&safesearch=true";

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/json")
                    .build();

            // execute() là synchronous → gọi từ executor (background thread)
            Response response = okHttpClient.newCall(request).execute();

            if (!response.isSuccessful() || response.body() == null) {
                int code = response.code();
                runOnUiThread(() -> toast("Pixabay error: " + code));
                return null;
            }

            JSONArray hits = new JSONObject(response.body().string()).getJSONArray("hits");
            List<String> result = new ArrayList<>();
            for (int i = 0; i < Math.min(10, hits.length()); i++)
                result.add(hits.getJSONObject(i).getString("webformatURL"));
            return result;

        } catch (Exception e) {
            runOnUiThread(() -> toast("Image error: " + e.getMessage()));
            return null;
        }
    }

    @Override public void onImageDelete(int position) { }

    private void saveAllUnsaved() {
        for (int i = 0; i < cardList.size(); i++) {
            FlashCardAdapter.CardData c = cardList.get(i);
            if ((c.isNew || c.id == null) && !TextUtils.isEmpty(c.term)) onSave(c, i);
        }
    }

    private String uriToBase64(Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096]; int n;
            while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
            byte[] bytes = bos.toByteArray();
            if (bytes.length > 3 * 1024 * 1024) { toast("Image too large (max 3MB)"); return null; }
            return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (IOException e) { toast("Failed to read image: " + e.getMessage()); return null; }
    }

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }

    private void showApiError(retrofit2.Response<?> r) {
        String msg = "Error " + r.code();
        try { if (r.errorBody() != null) msg = r.errorBody().string(); } catch (IOException ignored) {}
        toast(msg);
    }

    private String nvl(String s) { return s != null ? s : ""; }
}