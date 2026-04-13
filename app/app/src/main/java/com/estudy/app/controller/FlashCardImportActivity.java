package com.estudy.app.controller;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.estudy.app.R;
import com.estudy.app.api.ApiClient;
import com.estudy.app.api.ApiService;
import com.estudy.app.model.request.FlashCardImportRequest;
import com.estudy.app.model.response.ApiResponse;
import com.estudy.app.model.response.ImportResultResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.estudy.app.utils.TokenManager;

/**
 * UC-02: Batch import flashcards.
 * Format: "Term, Definition" per line.
 */
public class FlashCardImportActivity extends AppCompatActivity {

    public static final String EXTRA_SET_ID = "set_id";

    private EditText etContent;
    private TextView tvHint;
    private Button   btnImport, btnCancel;
    private String     setId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flash_card_import);

        setId = getIntent().getStringExtra(EXTRA_SET_ID);

        // Find the outer layout
        View toolbarLayout = findViewById(R.id.toolbar);

        // Find the sub-elements inside the layout (similar to your other activities)
        ImageButton btnBack = toolbarLayout.findViewById(R.id.btnBack);
        TextView tvTitle = toolbarLayout.findViewById(R.id.tvToolbarTitle);

        // Set the title and back button action
        if (tvTitle != null) {
            tvTitle.setText("Import Flashcards");
        }
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
        etContent = findViewById(R.id.etImportContent);
        tvHint    = findViewById(R.id.tvImportHint);
        btnImport = findViewById(R.id.btnImport);
        btnCancel = findViewById(R.id.btnCancelImport);

        tvHint.setText("Between Term and Definition: Comma (,)\nBetween cards: New line");

        // Example placeholder
        etContent.setHint(
                "Pen,  an instrument made of plastic or metal used for writing\n" +
                        "Book, a set of printed pages\n" +
                        "Ruler, a flat object for measuring");

        btnCancel.setOnClickListener(v -> finish());
        btnImport.setOnClickListener(v -> performImport());
    }

    private void performImport() {
        String content = etContent.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            etContent.setError("Please paste your data here");
            return;
        }

        btnImport.setEnabled(false);
        btnImport.setText("Importing...");

        // 1. Khởi tạo TokenManager (truyền 'this' vì đang ở trong Activity)
        TokenManager tokenManager = new TokenManager(this);

        // 2. Truyền tokenManager vào getInstance()
        ApiClient.getInstance(tokenManager).create(ApiService.class)
                .importFlashCards(setId, new FlashCardImportRequest(content))
                .enqueue(new Callback<ApiResponse<ImportResultResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<ImportResultResponse>> call,
                                           Response<ApiResponse<ImportResultResponse>> response) {
                        btnImport.setEnabled(true);
                        btnImport.setText("Import");

                        // LƯU Ý: Đổi từ getMessage().equals("Success") sang isSuccess() hoặc kiểm tra HTTP code
                        // Tuỳ thuộc vào cách bạn viết API Response
                        if (response.isSuccessful() && response.body() != null) {
                            ImportResultResponse result = response.body().getResult();
                            showResult(result);
                        } else {
                            Toast.makeText(FlashCardImportActivity.this,
                                    "Import failed", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<ImportResultResponse>> call, Throwable t) {
                        btnImport.setEnabled(true);
                        btnImport.setText("Import");
                        Toast.makeText(FlashCardImportActivity.this,
                                "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showResult(ImportResultResponse result) {
        StringBuilder msg = new StringBuilder();
        msg.append("✅ Imported: ").append(result.successCount).append(" cards\n");
        if (result.failedCount > 0) {
            msg.append("❌ Skipped: ").append(result.failedCount).append(" lines\n");
            if (result.errors != null && !result.errors.isEmpty()) {
                msg.append("\nDetails:\n");
                for (String err : result.errors) {
                    msg.append("• ").append(err).append("\n");
                }
            }
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Import Result")
                .setMessage(msg.toString())
                .setPositiveButton("Done", (d, w) -> {
                    // PHÁT TÍN HIỆU THÀNH CÔNG VỀ MÀN HÌNH DANH SÁCH
                    setResult(RESULT_OK);
                    finish();
                })
                .show();
    }
}