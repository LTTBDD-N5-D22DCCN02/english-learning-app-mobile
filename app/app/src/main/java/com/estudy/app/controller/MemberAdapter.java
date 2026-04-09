package com.estudy.app.controller;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.estudy.app.R;
import com.estudy.app.model.response.ClassMemberResponse;
import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.ViewHolder> {

    public interface OnMenuListener { void onMenu(ClassMemberResponse member); }

    private final List<ClassMemberResponse> items;
    private final boolean showMenu;           // true nếu viewer là LEADER
    private final OnMenuListener menuListener;

    public MemberAdapter(List<ClassMemberResponse> items,
                         boolean showMenu,
                         OnMenuListener menuListener) {
        this.items        = items;
        this.showMenu     = showMenu;
        this.menuListener = menuListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClassMemberResponse m = items.get(position);
        String displayName = m.getFullName() != null ? m.getFullName() : m.getUsername();
        holder.tvName.setText(displayName);

        // Fix #8: badge LEADER dạng text nhỏ bên dưới tên, không làm ô to hơn
        boolean isLeaderMember = "LEADER".equals(m.getRole());
        holder.tvRole.setVisibility(isLeaderMember ? View.VISIBLE : View.GONE);

        // Fix #9: hiện ngày tham gia
        if (m.getCreatedAt() != null && !m.getCreatedAt().isEmpty()) {
            // Cắt lấy phần ngày từ ISO string "2024-03-15T10:30:00"
            String dateOnly = m.getCreatedAt().length() >= 10
                    ? m.getCreatedAt().substring(0, 10) : m.getCreatedAt();
            holder.tvJoinDate.setText("Joined: " + dateOnly);
            holder.tvJoinDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvJoinDate.setVisibility(View.GONE);
        }

        // Fix #8: ẩn menu button cho chính thành viên LEADER
        // (Leader không tự xóa mình hoặc đổi quyền mình)
        if (showMenu && !isLeaderMember) {
            holder.btnMenu.setVisibility(View.VISIBLE);
            holder.btnMenu.setOnClickListener(v -> menuListener.onMenu(m));
        } else {
            holder.btnMenu.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return items == null ? 0 : items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvRole, tvJoinDate;
        ImageButton btnMenu;

        ViewHolder(View v) {
            super(v);
            tvName     = v.findViewById(R.id.tvMemberName);
            tvRole     = v.findViewById(R.id.tvRole);
            tvJoinDate = v.findViewById(R.id.tvJoinDate);
            btnMenu    = v.findViewById(R.id.btnMemberMore);
        }
    }
}