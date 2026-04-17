package com.estudy.app.controller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.estudy.app.R;
import com.estudy.app.model.response.NotificationResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

    public interface OnItemClickListener {
        void onItemClick(NotificationResponse notification);
    }

    private final List<NotificationResponse> items;
    private final OnItemClickListener listener;

    public NotificationAdapter(List<NotificationResponse> items, OnItemClickListener listener) {
        this.items    = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        NotificationResponse notif = items.get(position);
        holder.bind(notif, listener);
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ── ViewHolder ─────────────────────────────────────────────────
    static class VH extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvTime;
        private final TextView tvContent;
        private final View     viewUnreadDot;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle       = itemView.findViewById(R.id.tvTitle);
            tvTime        = itemView.findViewById(R.id.tvTime);
            tvContent     = itemView.findViewById(R.id.tvContent);
            viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);
        }

        void bind(NotificationResponse notif, OnItemClickListener listener) {
            tvTitle.setText(notif.getTitle() != null ? notif.getTitle() : mapTypeToLabel(notif.getType()));
            tvTime.setText("Time: " + formatTime(notif.getCreatedAt()));
            tvContent.setText("Content: " + (notif.getContent() != null ? notif.getContent() : ""));

            // Unread indicator
            if (!notif.isRead()) {
                viewUnreadDot.setVisibility(View.VISIBLE);
                tvTitle.setAlpha(1.0f);
            } else {
                viewUnreadDot.setVisibility(View.GONE);
                tvTitle.setAlpha(0.6f);
            }

            itemView.setOnClickListener(v -> listener.onItemClick(notif));
        }

        private String mapTypeToLabel(String type) {
            if (type == null) return "Notification";
            switch (type) {
                case "vocab_reminder": return "System Notification";
                case "join_request":   return "Class Notification";
                case "new_set":        return "Flashcard Set Notification";
                case "new_comment":    return "Flashcard Set Notification";
                default:               return "Notification";
            }
        }

        private String formatTime(String iso) {
            if (iso == null) return "";
            try {
                SimpleDateFormat input  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                SimpleDateFormat output = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
                Date date = input.parse(iso);
                return date != null ? output.format(date) : iso;
            } catch (ParseException e) {
                try {
                    SimpleDateFormat input2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                    SimpleDateFormat output = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
                    Date date = input2.parse(iso);
                    return date != null ? output.format(date) : iso;
                } catch (ParseException ex) {
                    return iso;
                }
            }
        }
    }
}
