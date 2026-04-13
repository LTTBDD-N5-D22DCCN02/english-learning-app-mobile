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
import androidx.fragment.app.FragmentManager;
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

    private ApiService   apiService;
    private OkHttpClient okHttpClient;
    private String       flashCardSetId;
    private String       flashCardSetName;
    private boolean      isEditMode;

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
                    else toast("Lỗi khi đọc ảnh!");
                }
                pendingImagePosition = -1;
            });

    private final ActivityResultLauncher<Intent> importLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    loadFlashCards(); // Tải lại danh sách
                }
                suppressNextResume = false;
            });

    // Bộ thu tín hiệu khi từ màn hình Edit (Sửa/Thêm) quay trở về màn hình View
    private final ActivityResultLauncher<Intent> editLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                // Nếu bên màn hình Edit có lưu, xóa hoặc đổi ảnh (RESULT_OK)
                if (result.getResultCode() == Activity.RESULT_OK) {
                    loadFlashCards(); // Gọi API tải lại danh sách thẻ mới nhất
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard_list);
        BottomNavHelper.setup(this, R.id.btnNavSets);

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
                i.putExtra(EXTRA_SET_ID,   flashCardSetId);
                i.putExtra(EXTRA_SET_NAME, flashCardSetName);
                i.putExtra(EXTRA_EDIT_MODE, true);

                editLauncher.launch(i);
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
                importLauncher.launch(i);
            });
        }

        loadFlashCards();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isEditMode && suppressNextResume) { suppressNextResume = false; return; }
        if (isEditMode) loadFlashCards();
    }

    @Override
    protected void onDestroy() { super.onDestroy(); executor.shutdownNow(); }

    // ── Load ──────────────────────────────────────────────────────────────────

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
                        // TẠO MỘT DANH SÁCH MỚI HOÀN TOÀN ĐỂ ĐƯỢC QUYỀN THÊM/XÓA
                        List<FlashCardResponse> serverCards = new ArrayList<>();
                        if (detail.getFlashCards() != null) {
                            serverCards.addAll(detail.getFlashCards());
                        }
                        if (isEditMode) {
                            cardList.clear();
                            for (FlashCardResponse r : serverCards)
                                cardList.add(new FlashCardAdapter.CardData(r));
                            adapter.notifyDataSetChanged();
                        } else {
                            // VIEW mode: Phải gán vào biến 'adapter' trước để không bị NULL khi xóa
                            adapter = new FlashCardAdapter(FlashCardListActivity.this, serverCards, FlashCardListActivity.this);
                            rvFlashCards.setAdapter(adapter);
                        }
                    }
                    @Override
                    public void onFailure(Call<ApiResponse<FlashCardSetDetailResponse>> call, Throwable t) {
                        toast("Load failed: " + t.getMessage());
                    }
                });
    }

    // ── onSave ────────────────────────────────────────────────────────────────

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
                        public void onFailure(Call<ApiResponse<FlashCardResponse>> call, Throwable t) {
                            toast("Network error");
                        }
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
                        public void onFailure(Call<ApiResponse<FlashCardResponse>> call, Throwable t) {
                            toast("Network error");
                        }
                    });
        }
    }

    // ── onDelete ──────────────────────────────────────────────────────────────

    @Override
    public void onDelete(FlashCardAdapter.CardData card, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete flashcard")
                .setMessage(TextUtils.isEmpty(card.term)
                        ? "Delete this card?" : "Delete \"" + card.term + "\"?")
                .setPositiveButton("Delete", (d, w) -> {
                    if (card.isNew || card.id == null) {
                        adapter.removeCard(position);
                    } else {
                        apiService.deleteFlashCard(card.id)
                                .enqueue(new Callback<ApiResponse<Void>>() {
                                    @Override
                                    public void onResponse(Call<ApiResponse<Void>> call,
                                                           retrofit2.Response<ApiResponse<Void>> r) {
                                        if (r.isSuccessful()) {
                                            adapter.removeCard(position);
                                            setResult(RESULT_OK);
                                            toast("Deleted");
                                        } else toast("Delete failed");
                                    }
                                    @Override
                                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                                        toast("Network error");
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", null).show();
    }

    // ── onSuggest — MULTI MEANING via SuggestDialog ───────────────────────────

    @Override
    public void onSuggest(int position, String term) {
        toast("Đang tra từ: " + term + "...");
        apiService.suggest(term).enqueue(new Callback<ApiResponse<SuggestResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<SuggestResponse>> call,
                                   retrofit2.Response<ApiResponse<SuggestResponse>> response) {
                if (!response.isSuccessful() || response.body() == null
                        || response.body().getResult() == null) {
                    // Log lỗi chi tiết như code gốc
                    String errorMsg = "Lỗi HTTP " + response.code();
                    try {
                        if (response.errorBody() != null)
                            errorMsg += " - " + response.errorBody().string();
                    } catch (Exception ignored) {}
                    toast(errorMsg);
                    android.util.Log.e("SUGGEST", errorMsg);
                    return;
                }

                SuggestResponse suggest = response.body().getResult();

                if (suggest.meanings == null || suggest.meanings.isEmpty()) {
                    toast("Không tìm thấy nghĩa cho \"" + term + "\"");
                    return;
                }

                // Mở bottom sheet hiển thị TẤT CẢ các nghĩa
                FragmentManager fm = getSupportFragmentManager();
                SuggestDialog.newInstance(suggest, (definition, ipa, example) -> {
                    // User chọn 1 nghĩa → điền vào card + refresh UI
                    adapter.applySuggestion(position, definition, ipa, example);
                    toast("Đã điền xong!");
                }).show(fm, "suggest_dialog");
            }

            @Override
            public void onFailure(Call<ApiResponse<SuggestResponse>> call, Throwable t) {
                toast("Lỗi kết nối Server: " + t.getMessage());
                android.util.Log.e("SUGGEST", "Network: " + t.getMessage());
            }
        });
    }

    // ── onImagePick ───────────────────────────────────────────────────────────

    @Override
    public void onImagePick(int position) {
        pendingImagePosition = position;
        imagePickerLauncher.launch(
                new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
    }

    // ── onImageSuggest ────────────────────────────────────────────────────────

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
            String url = "https://pixabay.com/api/?key=" + PIXABAY_KEY
                    + "&q=" + URLEncoder.encode(term, "UTF-8")
                    + "&image_type=photo&per_page=10&safesearch=true";
            Request request = new Request.Builder().url(url)
                    .addHeader("Accept", "application/json").build();
            Response response = okHttpClient.newCall(request).execute();
            if (!response.isSuccessful() || response.body() == null) return null;
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

    // ── Helpers ───────────────────────────────────────────────────────────────

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
        String msg = "Lỗi HTTP " + r.code();
        try {
            if (r.errorBody() != null) {
                String errorString = r.errorBody().string();
                try {
                    // Cố gắng bóc tách JSON lỗi của Spring Boot để lấy lời nhắn thực sự
                    JSONObject jsonObject = new JSONObject(errorString);
                    if (jsonObject.has("message")) {
                        msg = jsonObject.getString("message"); // Lấy dòng "A flashcard with this term already exists..."
                    } else if (jsonObject.has("error")) {
                        msg = jsonObject.getString("error");
                    } else {
                        msg = errorString;
                    }
                } catch (Exception e) {
                    // Nếu lỗi không phải dạng JSON thì in ra nguyên gốc
                    msg = errorString;
                }
            }
        } catch (IOException ignored) {}

        toast(msg); // Hiện thông báo lỗi chuẩn xác lên màn hình
    }
}