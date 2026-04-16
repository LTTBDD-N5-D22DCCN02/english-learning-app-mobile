package com.estudy.app.controller;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.estudy.app.R;
import com.estudy.app.model.response.ClassMemberResponse;
import java.util.List;

public class ApproveRequestAdapter extends RecyclerView.Adapter<ApproveRequestAdapter.ViewHolder> {

    public interface Action { void run(ClassMemberResponse member); }

    private final List<ClassMemberResponse> items;
    private final Action onAccept, onReject;

    public ApproveRequestAdapter(List<ClassMemberResponse> items, Action onAccept, Action onReject) {
        this.items = items;
        this.onAccept = onAccept;
        this.onReject = onReject;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_join_request, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClassMemberResponse m = items.get(position);
        String name = m.getFullName() != null ? m.getFullName() : m.getUsername();
        holder.tvName.setText(name);
        holder.btnAccept.setOnClickListener(v -> onAccept.run(m));
        holder.btnReject.setOnClickListener(v -> onReject.run(m));
    }

    @Override
    public int getItemCount() { return items == null ? 0 : items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        Button btnAccept, btnReject;
        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvRequesterName);
            btnAccept = v.findViewById(R.id.btnAccept);
            btnReject = v.findViewById(R.id.btnReject);
        }
    }
}