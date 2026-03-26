package com.estudy.app.controller;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.estudy.app.R;
import com.estudy.app.model.response.CommentResponse;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

    public interface OnDeleteClickListener {
        void onDelete(CommentResponse item);
    }

    private final List<CommentResponse> items;
    private final String currentUsername;
    private final String flashCardSetOwnerUsername;
    private final OnDeleteClickListener onDeleteClick;

    public CommentAdapter(List<CommentResponse> items,
                          String currentUsername,
                          String flashCardSetOwnerUsername,
                          OnDeleteClickListener onDeleteClick) {
        this.items = items;
        this.currentUsername = currentUsername;
        this.flashCardSetOwnerUsername = flashCardSetOwnerUsername;
        this.onDeleteClick = onDeleteClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommentResponse item = items.get(position);

        String name = item.getFullName() != null ? item.getFullName() : item.getUsername();
        holder.tvAvatar.setText(name != null && !name.isEmpty()
                ? String.valueOf(name.charAt(0)).toUpperCase() : "?");
        holder.tvFullName.setText(name);
        holder.tvContent.setText(item.getContent());

        // Chỉ hiện nút 3 chấm nếu là chủ comment hoặc chủ bộ flashcard
        boolean canDelete = currentUsername != null &&
                (currentUsername.equals(item.getUsername()) ||
                        currentUsername.equals(flashCardSetOwnerUsername));

        holder.tvDotMenu.setVisibility(canDelete ? View.VISIBLE : View.GONE);

        holder.tvDotMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.getMenu().add(0, 0, 0, "Delete");
            popup.setOnMenuItemClickListener(menuItem -> {
                onDeleteClick.onDelete(item);
                return true;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void removeItem(CommentResponse item) {
        int index = items.indexOf(item);
        if (index != -1) {
            items.remove(index);
            notifyItemRemoved(index);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvFullName, tvContent, tvDotMenu;

        ViewHolder(View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvFullName = itemView.findViewById(R.id.tvFullName);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvDotMenu = itemView.findViewById(R.id.tvDotMenu);
        }
    }
}

