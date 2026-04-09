package com.estudy.app.controller;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.estudy.app.R;
import com.estudy.app.model.response.ClassResponse;
import java.util.List;

public class PublicClassAdapter extends RecyclerView.Adapter<PublicClassAdapter.ViewHolder> {

    public interface OnJoinListener { void onJoin(ClassResponse cls); }

    private final List<ClassResponse> items;
    private final OnJoinListener joinListener;

    public PublicClassAdapter(List<ClassResponse> items, OnJoinListener joinListener) {
        this.items = items;
        this.joinListener = joinListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_public_class, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClassResponse cls = items.get(position);
        holder.tvName.setText(cls.getName());
        holder.tvMemberCount.setText("Members: " + cls.getMemberCount());
        holder.tvDescription.setText(cls.getDescription() != null ? cls.getDescription() : "");

        // Disable join button if already a member
        if (cls.getMyRole() != null) {
            holder.btnJoin.setText("Entered");
            holder.btnJoin.setEnabled(false);
        } else {
            holder.btnJoin.setText("Join");
            holder.btnJoin.setEnabled(true);
            holder.btnJoin.setOnClickListener(v -> joinListener.onJoin(cls));
        }
    }

    @Override
    public int getItemCount() { return items == null ? 0 : items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvMemberCount, tvDescription;
        Button btnJoin;
        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvClassName);
            tvMemberCount = v.findViewById(R.id.tvMemberCount);
            tvDescription = v.findViewById(R.id.tvDescription);
            btnJoin = v.findViewById(R.id.btnJoin);
        }
    }
}