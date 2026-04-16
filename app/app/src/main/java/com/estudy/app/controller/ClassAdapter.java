package com.estudy.app.controller;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.estudy.app.R;
import com.estudy.app.model.response.ClassResponse;
import java.util.List;

public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ViewHolder> {

    public interface OnClassClickListener {
        void onClick(ClassResponse cls);
    }

    public interface OnMenuClickListener {
        void onMenu(ClassResponse cls, View anchor);
    }

    private final List<ClassResponse> items;
    private final OnClassClickListener clickListener;
    private final OnMenuClickListener menuListener;

    public ClassAdapter(List<ClassResponse> items,
                        OnClassClickListener clickListener,
                        OnMenuClickListener menuListener) {
        this.items = items;
        this.clickListener = clickListener;
        this.menuListener = menuListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_class, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClassResponse cls = items.get(position);
        holder.tvName.setText(cls.getName());
        holder.tvMemberCount.setText("Number of members: " + cls.getMemberCount());
        holder.tvDescription.setText("Description: " + (cls.getDescription() != null ? cls.getDescription() : ""));
        holder.itemView.setOnClickListener(v -> clickListener.onClick(cls));
        holder.tvDotMenu.setOnClickListener(v -> menuListener.onMenu(cls, v));
    }

    @Override
    public int getItemCount() { return items == null ? 0 : items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvMemberCount, tvDescription, tvDotMenu;
        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvClassName);
            tvMemberCount = v.findViewById(R.id.tvMemberCount);
            tvDescription = v.findViewById(R.id.tvDescription);
            tvDotMenu = v.findViewById(R.id.tvDotMenu);
        }
    }
}