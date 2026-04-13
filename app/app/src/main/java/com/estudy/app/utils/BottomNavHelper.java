package com.estudy.app.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;
import com.estudy.app.R;
import com.estudy.app.controller.FlashCardSetCreateActivity;
import com.estudy.app.controller.FlashCardSetListActivity;
import com.estudy.app.controller.HomeActivity;
import com.estudy.app.controller.StatisticsActivity;

public class BottomNavHelper {

    public static void setup(Activity activity, int activeTabId) {
        setupBtn(activity, R.id.btnNavHome,  activeTabId, () ->
                NavHelper.goHome(activity));

        setupBtn(activity, R.id.btnNavSets,  activeTabId, () ->
                NavHelper.go(activity, new Intent(activity, FlashCardSetListActivity.class)));

        setupBtn(activity, R.id.btnNavAdd,   activeTabId, () ->
                NavHelper.go(activity, new Intent(activity, FlashCardSetCreateActivity.class)));

        setupBtn(activity, R.id.btnNavNotif, activeTabId, () ->
                Toast.makeText(activity, "Notifications — coming soon!", Toast.LENGTH_SHORT).show());

        setupBtn(activity, R.id.btnNavStats, activeTabId, () ->
                NavHelper.go(activity, new Intent(activity, StatisticsActivity.class)));
    }

    private static void setupBtn(Activity activity, int btnId, int activeTabId, Runnable action) {
        View btn = activity.findViewById(btnId);
        if (btn == null) return;
        btn.setAlpha(btnId == activeTabId ? 1.0f : 0.55f);
        btn.setOnClickListener(v -> action.run());
    }
}