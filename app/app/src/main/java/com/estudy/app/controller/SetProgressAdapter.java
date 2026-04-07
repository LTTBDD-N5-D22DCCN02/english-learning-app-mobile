package com.estudy.app.controller;

import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.estudy.app.R;
import com.estudy.app.model.response.SetProgressResponse;
import java.util.List;

public class SetProgressAdapter extends RecyclerView.Adapter<SetProgressAdapter.VH> {

    private final List<SetProgressResponse> items;

    public SetProgressAdapter(List<SetProgressResponse> items) { this.items = items; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_set_progress, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        SetProgressResponse item = items.get(position);

        h.tvSetName.setText(item.getSetName());
        h.tvPercent.setText(String.format("%.0f%%", item.getPercentage()));
        h.progressBar.setProgress((int) item.getPercentage());
        h.tvRemembered.setText(item.getRememberedCount() + " nhớ");
        h.tvNotYet.setText(item.getNotYetCount() + " chưa nhớ");
        h.tvNotStudied.setText(item.getNotStudiedCount() + " chưa học");
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView    tvSetName, tvPercent, tvRemembered, tvNotYet, tvNotStudied;
        ProgressBar progressBar;

        VH(View v) {
            super(v);
            tvSetName    = v.findViewById(R.id.tvSetName);
            tvPercent    = v.findViewById(R.id.tvPercent);
            progressBar  = v.findViewById(R.id.progressBar);
            tvRemembered = v.findViewById(R.id.tvRemembered);
            tvNotYet     = v.findViewById(R.id.tvNotYet);
            tvNotStudied = v.findViewById(R.id.tvNotStudied);
        }
    }
}
