package com.estudy.app.controller;

import android.content.Context;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.estudy.app.R;
import com.estudy.app.model.response.StudySetItem;
import java.util.List;

public class StudySetAdapter extends RecyclerView.Adapter<StudySetAdapter.ViewHolder> {

    public interface OnSetClickListener {
        void onSetClick(StudySetItem item);
    }

    private final List<StudySetItem> items;
    private final boolean isDueList;
    private final OnSetClickListener listener;

    public StudySetAdapter(List<StudySetItem> items, boolean isDueList,
                           OnSetClickListener listener) {
        this.items    = items;
        this.isDueList = isDueList;
        this.listener  = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_study_set, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        StudySetItem item = items.get(position);
        Context ctx = h.itemView.getContext();

        h.tvSetName.setText(item.getSetName());
        h.tvSetSubtitle.setText(item.getLastReviewedAt());
        h.tvWordCount.setText(item.getWordCount() + " words");

        // Progress bar
        int progress = item.getTotalWords() > 0
                ? (int) ((float) item.getRememberedCount() / item.getTotalWords() * 100)
                : 0;
        h.progressBar.setProgress(progress);

        // Preview từ
        h.layoutWordPreview.removeAllViews();
        List<String> terms = item.getPreviewTerms();
        if (terms != null && !terms.isEmpty()) {
            int shown = Math.min(terms.size(), 3);
            for (int i = 0; i < shown; i++) {
                h.layoutWordPreview.addView(makeChip(ctx, terms.get(i)));
            }
            int remaining = item.getWordCount() - shown;
            if (remaining > 0) {
                h.layoutWordPreview.addView(makeMore(ctx, "+" + remaining + " more"));
            }
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSetClick(item);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    private TextView makeChip(Context ctx, String text) {
        TextView tv = new TextView(ctx);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(ctx, 6));
        tv.setLayoutParams(lp);
        tv.setText(text);
        tv.setTextSize(10f);
        tv.setTextColor(Color.parseColor("#475569"));
        tv.setBackgroundColor(Color.parseColor("#F1F5F9"));
        tv.setPadding(dp(ctx, 8), dp(ctx, 3), dp(ctx, 8), dp(ctx, 3));
        return tv;
    }

    private TextView makeMore(Context ctx, String text) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(10f);
        tv.setTextColor(Color.parseColor("#94a3b8"));
        tv.setPadding(dp(ctx, 2), dp(ctx, 3), 0, dp(ctx, 3));
        return tv;
    }

    private int dp(Context ctx, int dp) {
        return (int) (dp * ctx.getResources().getDisplayMetrics().density);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSetName, tvSetSubtitle, tvWordCount;
        ProgressBar progressBar;
        LinearLayout layoutWordPreview;

        ViewHolder(View v) {
            super(v);
            tvSetName         = v.findViewById(R.id.tvSetName);
            tvSetSubtitle     = v.findViewById(R.id.tvSetSubtitle);
            tvWordCount       = v.findViewById(R.id.tvWordCount);
            progressBar       = v.findViewById(R.id.progressBar);
            layoutWordPreview = v.findViewById(R.id.layoutWordPreview);
        }
    }
}