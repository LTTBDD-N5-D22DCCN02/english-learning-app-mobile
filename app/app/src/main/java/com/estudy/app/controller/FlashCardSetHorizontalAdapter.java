package com.estudy.app.controller;

import android.content.Context;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.estudy.app.R;
import com.estudy.app.model.response.FlashCardSetResponse;
import java.util.List;

/**
 * Adapter cho carousel ngang "My Flashcard Sets" trên màn hình Home.
 * Layout: item_flashcard_set_horizontal.xml (card 160dp x 120dp)
 */
public class FlashCardSetHorizontalAdapter
        extends RecyclerView.Adapter<FlashCardSetHorizontalAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(FlashCardSetResponse item);
    }

    private final List<FlashCardSetResponse> items;
    private final OnItemClickListener listener;

    public FlashCardSetHorizontalAdapter(List<FlashCardSetResponse> items,
                                         OnItemClickListener listener) {
        this.items    = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_flashcard_set_horizontal, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        FlashCardSetResponse item = items.get(position);

        h.tvName.setText(item.getName());

        // Card count — hiện tại API trả về FlashCardSetResponse chưa có cardCount
        // TODO: thêm field cardCount vào FlashCardSetResponse khi Vân cập nhật
        h.tvCardCount.setText("0 terms");

        // Privacy badge
        String privacy = item.getPrivacy();
        h.tvPrivacy.setText(privacy != null ? privacy : "Public");

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(item);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCardCount, tvPrivacy;

        ViewHolder(View v) {
            super(v);
            tvName      = v.findViewById(R.id.tvName);
            tvCardCount = v.findViewById(R.id.tvCardCount);
            tvPrivacy   = v.findViewById(R.id.tvPrivacy);
        }
    }
}