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
        this.items         = items;
        this.onItemClick   = onItemClick;
        this.onEditClick   = onEditClick;
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
        holder.tvDescription.setText(
                item.getDescription() != null && !item.getDescription().isEmpty()
                        ? item.getDescription() : "No description");
        holder.tvPrivacy.setText(
                "Privacy: " + (item.getPrivacy() != null
                        ? item.getPrivacy().toLowerCase() : "-"));

        // FIX 2: Click item → xem danh sách flashcard (VIEW mode)
        holder.itemView.setOnClickListener(v -> {
            if (onItemClick != null) onItemClick.onClick(item);
        });

        // FIX 2: 3 chấm → popup có đủ 3 tùy chọn
        holder.tvDotMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            // Thêm đủ 3 menu items
            popup.getMenu().add(0, R.id.menu_view,   0, "View flashcards");
            popup.getMenu().add(0, R.id.menu_edit,   1, "Edit");
            popup.getMenu().add(0, R.id.menu_delete, 2, "Delete");

            popup.setOnMenuItemClickListener(menuItem -> {
                int id = menuItem.getItemId();
                if (id == R.id.menu_view) {
                    if (onItemClick != null) onItemClick.onClick(item);
                } else if (id == R.id.menu_edit) {
                    if (onEditClick != null) onEditClick.onClick(item);
                } else if (id == R.id.menu_delete) {
                    if (onDeleteClick != null) onDeleteClick.onClick(item);
                }
                return true;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    /** Xóa item khỏi list sau khi API delete thành công */
    public void removeItem(int position) {
        items.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, items.size());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDescription, tvPrivacy, tvDotMenu;

        ViewHolder(View itemView) {
            super(itemView);
            tvName        = itemView.findViewById(R.id.tvName);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvPrivacy     = itemView.findViewById(R.id.tvPrivacy);
            tvDotMenu     = itemView.findViewById(R.id.tvDotMenu);
        }
    }
}