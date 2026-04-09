package com.estudy.app.controller;

import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.estudy.app.R;
import com.estudy.app.model.response.FlashCardResponse;
import java.util.List;

public class FlashCardDetailAdapter
        extends RecyclerView.Adapter<FlashCardDetailAdapter.ViewHolder> {

    public interface OnDotMenuClick { void onClick(FlashCardResponse item); }

    private final List<FlashCardResponse> items;
    private final OnDotMenuClick dotMenuClick;
    private TextToSpeech tts;

    public FlashCardDetailAdapter(List<FlashCardResponse> items, OnDotMenuClick dotMenuClick) {
        this.items = items;
        this.dotMenuClick = dotMenuClick;
    }

    public void setTts(TextToSpeech tts) {
        this.tts = tts;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_flashcard, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Log.d("TEST", "size = " + items.size());
        FlashCardResponse item = items.get(position);
        h.tvTerm.setText(item.getTerm() != null ? item.getTerm() : "");
        h.tvIpa.setText(item.getIpa() != null ? item.getIpa() : "");
        h.tvDefinition.setText(item.getDefinition() != null ? item.getDefinition() : "");

        h.btnDotMenu.setOnClickListener(v -> {
            if (dotMenuClick != null) dotMenuClick.onClick(item);
        });


        // Audio: FlashCardResponse chưa có audioUrl → dùng TTS trực tiếp
        h.btnAudio.setOnClickListener(v -> speakWithTTS(item.getTerm()));
    }

    private void speakWithTTS(String term) {
        if (tts != null && term != null && !term.isEmpty()) {
            tts.speak(term, TextToSpeech.QUEUE_FLUSH, null, "tts_" + term);
        }
    }

    @Override public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTerm, tvIpa, tvDefinition;
        ImageButton btnDotMenu, btnAudio;
        ViewHolder(View v) {
            super(v);
            tvTerm       = v.findViewById(R.id.tvTerm);
            tvIpa        = v.findViewById(R.id.tvIpa);
            tvDefinition = v.findViewById(R.id.tvDefinition);
            btnDotMenu   = v.findViewById(R.id.btnDotMenu);
            btnAudio     = v.findViewById(R.id.btnAudio);
        }
    }
}