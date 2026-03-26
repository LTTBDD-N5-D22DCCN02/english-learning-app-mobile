package com.estudy.app.controller;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.estudy.app.R;
import com.estudy.app.model.response.FlashCardSetResponse;
import java.util.List;

public class FlashCardSetAdapter extends RecyclerView.Adapter<FlashCardSetAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(FlashCardSetResponse item);
    }

    private final List<FlashCardSetResponse> items;
    private final OnItemClickListener onItemClick;
    private final OnItemClickListener onEditClick;
    private final OnItemClickListener onDeleteClick;

    public FlashCardSetAdapter(List<FlashCardSetResponse> items,
                               OnItemClickListener onItemClick,
                               OnItemClickListener onEditClick,
                               OnItemClickListener onDeleteClick) {
        this.items = items;
        this.onItemClick = onItemClick;
        this.onEditClick = onEditClick;
        this.onDeleteClick = onDeleteClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_flashcard_set, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FlashCardSetResponse item = items.get(position);

        holder.tvName.setText(item.getName());
        holder.tvDescription.setText("Description: " + (item.getDescription() != null ? item.getDescription() : "-"));
        holder.tvPrivacy.setText("Privacy: " + (item.getPrivacy() != null ? item.getPrivacy().toLowerCase() : "-"));

        holder.itemView.setOnClickListener(v -> onItemClick.onClick(item));

        holder.tvDotMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.getMenu().add(0, 0, 0, "Edit");
            popup.getMenu().add(0, 1, 1, "Delete");
            popup.setOnMenuItemClickListener(menuItem -> {
                if (menuItem.getItemId() == 0) {
                    onEditClick.onClick(item);
                } else {
                    onDeleteClick.onClick(item);
                }
                return true;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDescription, tvPrivacy, tvDotMenu;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvPrivacy = itemView.findViewById(R.id.tvPrivacy);
            tvDotMenu = itemView.findViewById(R.id.tvDotMenu);
        }
    }
}