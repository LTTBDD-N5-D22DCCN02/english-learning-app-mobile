package com.estudy.app.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import com.estudy.app.R;
import com.estudy.app.api.ApiService;
import com.estudy.app.controller.FlashCardSetCreateActivity;
import com.estudy.app.controller.FlashCardSetListActivity;
import com.estudy.app.controller.NotificationActivity;
import com.estudy.app.controller.StatisticsActivity;
import com.estudy.app.model.response.ApiResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BottomNavHelper {

    public static void setup(Activity activity, int activeTabId) {
        setupBtn(activity, R.id.btnNavHome,  activeTabId, () ->
                NavHelper.goHome(activity));

        setupBtn(activity, R.id.btnNavSets,  activeTabId, () ->
                NavHelper.go(activity, new Intent(activity, FlashCardSetListActivity.class)));

        setupBtn(activity, R.id.btnNavAdd,   activeTabId, () ->
                NavHelper.go(activity, new Intent(activity, FlashCardSetCreateActivity.class)));

        setupBtn(activity, R.id.btnNavNotif, activeTabId, () ->
                NavHelper.go(activity, new Intent(activity, NotificationActivity.class)));

        setupBtn(activity, R.id.btnNavStats, activeTabId, () ->
                NavHelper.go(activity, new Intent(activity, StatisticsActivity.class)));
    }

    /** Gọi trong onResume() của activity để cập nhật badge số thông báo chưa đọc */
    public static void loadBadge(Activity activity, ApiService apiService) {
        TextView tvBadge = activity.findViewById(R.id.tvNotifBadge);
        if (tvBadge == null) return;

        apiService.getUnreadNotificationCount().enqueue(new Callback<ApiResponse<Long>>() {
            @Override
            public void onResponse(Call<ApiResponse<Long>> call,
                                   Response<ApiResponse<Long>> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                Long count = response.body().getResult();
                if (count == null) return;

                activity.runOnUiThread(() -> {
                    if (count <= 0) {
                        tvBadge.setVisibility(View.GONE);
                    } else {
                        tvBadge.setVisibility(View.VISIBLE);
                        tvBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                    }
                });
            }

            @Override
            public void onFailure(Call<ApiResponse<Long>> call, Throwable t) {
                // Silent fail — badge ẩn đi
                activity.runOnUiThread(() -> tvBadge.setVisibility(View.GONE));
            }
        });
    }

    private static void setupBtn(Activity activity, int btnId, int activeTabId, Runnable action) {
        View btn = activity.findViewById(btnId);
        if (btn == null) return;
        btn.setAlpha(btnId == activeTabId ? 1.0f : 0.55f);
        btn.setOnClickListener(v -> action.run());
    }
}