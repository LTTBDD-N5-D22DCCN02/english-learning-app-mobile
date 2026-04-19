package com.estudy.app.controller;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.estudy.app.R;
import com.estudy.app.bot.BotMessage;
import java.util.List;

public class BotMessageAdapter extends RecyclerView.Adapter<BotMessageAdapter.MsgHolder> {

    private static final int VIEW_USER = 0;
    private static final int VIEW_BOT  = 1;

    private final List<BotMessage> messages;

    public BotMessageAdapter(List<BotMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType() == BotMessage.Type.USER ? VIEW_USER : VIEW_BOT;
    }

    @NonNull
    @Override
    public MsgHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == VIEW_USER)
                ? R.layout.item_message_user
                : R.layout.item_message_bot;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new MsgHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MsgHolder holder, int position) {
        holder.tvText.setText(messages.get(position).getText());
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class MsgHolder extends RecyclerView.ViewHolder {
        final TextView tvText;
        MsgHolder(View view) {
            super(view);
            tvText = view.findViewById(R.id.tvMessageText);
        }
    }
}
