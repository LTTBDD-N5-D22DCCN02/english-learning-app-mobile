package com.estudy.app.utils;

import android.app.Activity;
import android.content.Intent;
import com.estudy.app.R;

/**
 * Điều hướng mượt với animation slide.
 *
 * Dùng thay startActivity():
 *   NavHelper.go(this, new Intent(this, HomeActivity.class));
 *
 * Back button:
 *   NavHelper.back(this);
 */
public class NavHelper {

    /** Mở Activity mới với animation slide từ phải vào */
    public static void go(Activity from, Intent intent) {
        from.startActivity(intent);
        from.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    /** Quay lại Activity trước với animation slide sang phải */
    public static void back(Activity activity) {
        activity.finish();
        activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    /** Mở HomeActivity và clear stack */
    public static void goHome(Activity from) {
        Intent i = new Intent(from,
                com.estudy.app.controller.HomeActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        go(from, i);
    }
}