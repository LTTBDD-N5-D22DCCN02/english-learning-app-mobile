package com.estudy.app.controller;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.estudy.app.R;
import com.estudy.app.model.response.FlashCardSetResponse;
import java.util.List;

public class ClassFlashcardSetAdapter
        extends RecyclerView.Adapter<ClassFlashcardSetAdapter.ViewHolder> {

    public interface OnMenuListener  { void onMenu(FlashCardSetResponse set); }
    public interface OnClickListener { void onClick(FlashCardSetResponse set); }

    private final List<FlashCardSetResponse> items;
    private final boolean isLeader;
    private final String  currentUserId;
    private final OnMenuListener  menuListener;
    private       OnClickListener clickListener;

    // Constructor cũ (backward compat)
    public ClassFlashcardSetAdapter(List<FlashCardSetResponse> items,
                                    boolean isLeader,
                                    String currentUserId,
                                    OnMenuListener menuListener) {
        this.items         = items;
        this.isLeader      = isLeader;
        this.currentUserId = currentUserId;
        this.menuListener  = menuListener;
    }

    public void setOnClickListener(OnClickListener l) { this.clickListener = l; }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_flashcard_set, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FlashCardSetResponse s = items.get(position);

        holder.tvName.setText(s.getName());

        // Hiển thị số thẻ
        if (s.getCardCount() > 0) {
            holder.tvCardCount.setText(s.getCardCount() + " cards");
            holder.tvCardCount.setVisibility(View.VISIBLE);
        } else {
            holder.tvCardCount.setVisibility(View.GONE);
        }

        // Hiển thị description
        if (s.getDescription() != null && !s.getDescription().isEmpty()) {
            holder.tvDescription.setText(s.getDescription());
            holder.tvDescription.setVisibility(View.VISIBLE);
        } else {
            holder.tvDescription.setVisibility(View.GONE);
        }

        // Hiển thị privacy
        holder.tvPrivacy.setText("Privacy: " + (s.getPrivacy() != null ? s.getPrivacy().toLowerCase() : "private"));

        // Click vào item → mở xem thẻ
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onClick(s);
        });

        boolean isOwner = s.getOwnerId() != null && s.getOwnerId().equals(currentUserId);
        if (isLeader || isOwner) {
            holder.tvDotMenu.setVisibility(View.VISIBLE);
            holder.tvDotMenu.setOnClickListener(v -> menuListener.onMenu(s));
        } else {
            holder.tvDotMenu.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return items == null ? 0 : items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCardCount, tvDescription, tvPrivacy, tvDotMenu;
        ViewHolder(View v) {
            super(v);
            tvName        = v.findViewById(R.id.tvName);
            tvCardCount   = v.findViewById(R.id.tvCardCount);
            tvDescription = v.findViewById(R.id.tvDescription);
            tvPrivacy     = v.findViewById(R.id.tvPrivacy);
            tvDotMenu     = v.findViewById(R.id.tvDotMenu);
        }
    }
}