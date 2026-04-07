package com.estudy.app.utils;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import com.estudy.app.model.response.DayActivityResponse;

import java.util.List;

/**
 * Custom View vẽ biểu đồ cột hoạt động học từ vựng mỗi ngày.
 * Dùng cho UC-STAT-04,05.
 */
public class StudyBarChartView extends View {

    private List<DayActivityResponse> data;
    private int maxWordCount = 1;

    private final Paint paintBar     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBarBg   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintLabel   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintCount   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect         = new RectF();

    public StudyBarChartView(Context context) {
        super(context); init();
    }

    public StudyBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs); init();
    }

    public StudyBarChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); init();
    }

    private void init() {
        paintBar.setColor(Color.parseColor("#005AAE"));
        paintBarBg.setColor(Color.parseColor("#EBF4FF"));
        paintLabel.setColor(Color.parseColor("#94a3b8"));
        paintLabel.setTextSize(sp(9));
        paintLabel.setTextAlign(Paint.Align.CENTER);
        paintCount.setColor(Color.parseColor("#374151"));
        paintCount.setTextSize(sp(8));
        paintCount.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(List<DayActivityResponse> data) {
        this.data = data;
        maxWordCount = 1;
        if (data != null) {
            for (DayActivityResponse d : data) {
                if (d.getWordCount() > maxWordCount) maxWordCount = d.getWordCount();
            }
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data == null || data.isEmpty()) return;

        int w = getWidth();
        int h = getHeight();

        float paddingH   = dp(10);
        float labelH     = sp(12);
        float countH     = sp(10);
        float chartTop   = countH + dp(4);
        float chartBottom = h - labelH - dp(6);
        float chartH     = chartBottom - chartTop;

        int n = data.size();
        float barAreaW = (w - paddingH * 2) / n;
        float barW     = barAreaW * 0.55f;
        float cornerR  = dp(3);

        for (int i = 0; i < n; i++) {
            DayActivityResponse day = data.get(i);
            float cx = paddingH + i * barAreaW + barAreaW / 2f;
            float barH  = chartH * day.getWordCount() / (float) maxWordCount;
            float barTop = chartBottom - barH;

            // Background bar (light)
            rect.set(cx - barW / 2, chartTop, cx + barW / 2, chartBottom);
            canvas.drawRoundRect(rect, cornerR, cornerR, paintBarBg);

            // Actual bar
            if (day.getWordCount() > 0) {
                rect.set(cx - barW / 2, barTop, cx + barW / 2, chartBottom);
                // Color by accuracy
                double acc = day.getAccuracyPercent();
                if (acc >= 80)      paintBar.setColor(Color.parseColor("#4CAF50"));
                else if (acc >= 50) paintBar.setColor(Color.parseColor("#005AAE"));
                else                paintBar.setColor(Color.parseColor("#E24B4A"));
                canvas.drawRoundRect(rect, cornerR, cornerR, paintBar);

                // Word count on top
                if (day.getWordCount() > 0) {
                    canvas.drawText(String.valueOf(day.getWordCount()),
                            cx, barTop - dp(2), paintCount);
                }
            }

            // Day label at bottom (Mon, Tue, etc. or abbreviated date)
            String label = formatLabel(day.getDate(), n);
            canvas.drawText(label, cx, h - dp(2), paintLabel);
        }
    }

    private String formatLabel(String date, int total) {
        if (date == null || date.length() < 10) return "";
        try {
            // date format: "2025-01-15"
            int month = Integer.parseInt(date.substring(5, 7));
            int day   = Integer.parseInt(date.substring(8, 10));
            if (total <= 7) {
                // Show day of week abbreviation
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(
                    Integer.parseInt(date.substring(0,4)), month - 1, day);
                String[] days = {"Su","Mo","Tu","We","Th","Fr","Sa"};
                return days[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1];
            } else {
                return month + "/" + day;
            }
        } catch (Exception e) {
            return "";
        }
    }

    private float dp(int dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float sp(int sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }
}
