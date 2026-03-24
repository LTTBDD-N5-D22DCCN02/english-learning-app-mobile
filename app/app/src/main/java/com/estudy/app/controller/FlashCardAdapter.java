package com.estudy.app.controller;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.estudy.app.R;
import com.estudy.app.model.response.FlashCardResponse;
import java.util.List;

public class FlashCardAdapter extends RecyclerView.Adapter<FlashCardAdapter.ViewHolder> {

    private final List<FlashCardResponse> items;

    public FlashCardAdapter(List<FlashCardResponse> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_flashcard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FlashCardResponse item = items.get(position);
        holder.tvTerm.setText(item.getTerm());
        holder.tvIpa.setText(item.getIpa() != null ? "/" + item.getIpa() + "/" : "");
        holder.tvDefinition.setText(item.getDefinition() != null ? item.getDefinition() : "");
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTerm, tvIpa, tvDefinition, tvDotMenu;

        ViewHolder(View itemView) {
            super(itemView);
            tvTerm = itemView.findViewById(R.id.tvTerm);
            tvIpa = itemView.findViewById(R.id.tvIpa);
            tvDefinition = itemView.findViewById(R.id.tvDefinition);
            tvDotMenu = itemView.findViewById(R.id.tvDotMenu);
        }
    }
}