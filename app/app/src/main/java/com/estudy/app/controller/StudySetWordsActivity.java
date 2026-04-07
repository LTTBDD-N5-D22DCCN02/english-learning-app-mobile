package com.estudy.app.controller;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.estudy.app.R;
import com.estudy.app.api.ApiClient;
import com.estudy.app.api.ApiService;
import com.estudy.app.model.response.ApiResponse;
import com.estudy.app.model.response.FlashCardResponse;
import com.estudy.app.model.response.FlashCardSetDetailResponse;
import com.estudy.app.utils.TokenManager;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudySetWordsActivity extends AppCompatActivity {

    public static final String EXTRA_SET_ID = "setId";
    public static final String EXTRA_SET_NAME = "setName";
    public static final String EXTRA_IS_DUE = "isDue";

    private RecyclerView rvWords;
    private TextView tvSetName, tvSectionLabel, tvWordCount;
    private ImageView btnStudyMode;
    private ApiService apiService;
    private String setId;
    private String setName;
    private boolean isDue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_set_words);

        setId = getIntent().getStringExtra(EXTRA_SET_ID);
        setName = getIntent().getStringExtra(EXTRA_SET_NAME);
        isDue = getIntent().getBooleanExtra(EXTRA_IS_DUE, true);

        TokenManager tm = new TokenManager(this);
        apiService = ApiClient.getInstance(tm).create(ApiService.class);

        tvSetName = findViewById(R.id.tvSetName);
        tvSectionLabel = findViewById(R.id.tvSectionLabel);
        tvWordCount = findViewById(R.id.tvWordCount);
        btnStudyMode = findViewById(R.id.btnStudyMode);
        rvWords = findViewById(R.id.rvWords);

        tvSetName.setText(setName != null ? setName : "");

        if (isDue) {
            tvSectionLabel.setText("Due today");
            tvSectionLabel.setTextColor(0xFFE24B4A);
        } else {
            tvSectionLabel.setText("New words");
            tvSectionLabel.setTextColor(0xFF378ADD);
        }

        rvWords.setLayoutManager(new LinearLayoutManager(this));

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        btnStudyMode.setOnClickListener(v -> {
            StudyModeBottomSheet sheet = StudyModeBottomSheet.newInstance(setId, setName);
            sheet.show(getSupportFragmentManager(), "StudyMode");
        });

        loadWords();
    }

    private void loadWords() {
        apiService.getFlashCardSetDetail(setId).enqueue(new Callback<ApiResponse<FlashCardSetDetailResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<FlashCardSetDetailResponse>> call,
                                   Response<ApiResponse<FlashCardSetDetailResponse>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getResult() != null) {
                    List<FlashCardResponse> cards = response.body().getResult().getFlashCards();
                    if (cards == null) cards = new ArrayList<>();
                    tvWordCount.setText(cards.size() + " words");
                    rvWords.setAdapter(new WordAdapter(cards));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<FlashCardSetDetailResponse>> call, Throwable t) {
                Toast.makeText(StudySetWordsActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Simple RecyclerView adapter for word list ──────────────────────
    static class WordAdapter extends RecyclerView.Adapter<WordAdapter.ViewHolder> {

        private final List<FlashCardResponse> items;

        WordAdapter(List<FlashCardResponse> items) { this.items = items; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_study_card_word, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int position) {
            FlashCardResponse card = items.get(position);
            h.tvTerm.setText(card.getTerm() != null ? card.getTerm() : "");
            h.tvIpa.setText(card.getIpa() != null ? " /" + card.getIpa() + "/" : "");
            h.tvDefinition.setText(card.getDefinition() != null ? card.getDefinition() : "");

            h.btnSpeaker.setOnClickListener(v ->
                    Toast.makeText(v.getContext(), "Audio — coming soon!", Toast.LENGTH_SHORT).show());
            h.btnMore.setOnClickListener(v ->
                    Toast.makeText(v.getContext(), "More options — coming soon!", Toast.LENGTH_SHORT).show());
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTerm, tvIpa, tvDefinition;
            ImageButton btnSpeaker, btnMore;

            ViewHolder(View v) {
                super(v);
                tvTerm = v.findViewById(R.id.tvTerm);
                tvIpa = v.findViewById(R.id.tvIpa);
                tvDefinition = v.findViewById(R.id.tvDefinition);
                btnSpeaker = v.findViewById(R.id.btnSpeaker);
                btnMore = v.findViewById(R.id.btnMore);
            }
        }
    }
}
