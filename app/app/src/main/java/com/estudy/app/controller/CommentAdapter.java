package com.estudy.app.controller;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.estudy.app.R;
import com.estudy.app.model.response.CommentResponse;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

    private final List<CommentResponse> items;

    public CommentAdapter(List<CommentResponse> items) {
        this.items = items;
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

        // Hiển thị chữ cái đầu của tên làm avatar
        String name = item.getFullName() != null ? item.getFullName() : item.getUsername();
        holder.tvAvatar.setText(name != null && !name.isEmpty()
                ? String.valueOf(name.charAt(0)).toUpperCase() : "?");
        holder.tvFullName.setText(name);
        holder.tvContent.setText(item.getContent());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvFullName, tvContent;

        ViewHolder(View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvFullName = itemView.findViewById(R.id.tvFullName);
            tvContent = itemView.findViewById(R.id.tvContent);
        }
    }
}