package com.estudy.app.utils;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

/**
 * Biểu đồ tròn (donut chart) hiển thị tiến độ học flashcard.
 * 3 phần: Remembered (xanh lá), Need review (đỏ), Not studied (xám)
 *
 * Dùng trong XML:
 *   <com.estudy.app.utils.StudyProgressView
 *       android:id="@+id/studyProgressView"
 *       android:layout_width="140dp"
 *       android:layout_height="140dp" />
 *
 * Set data từ Java:
 *   studyProgressView.setData(remembered, needReview, notStudied);
 */
public class StudyProgressView extends View {

    private int remembered = 0;
    private int needReview = 0;
    private int notStudied = 0;

    private final Paint paintRemembered = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintNeedReview = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintNotStudied  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintCenter      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintText        = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintSubText     = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF oval = new RectF();

    public StudyProgressView(Context context) {
        super(context); init();
    }
    public StudyProgressView(Context context, AttributeSet attrs) {
        super(context, attrs); init();
    }
    public StudyProgressView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle); init();
    }

    private void init() {
        float d = getResources().getDisplayMetrics().density;

        paintRemembered.setColor(0xFF4CAF50); // xanh lá
        paintRemembered.setStyle(Paint.Style.STROKE);
        paintRemembered.setStrokeWidth(16 * d);
        paintRemembered.setStrokeCap(Paint.Cap.BUTT);

        paintNeedReview.setColor(0xFFE24B4A); // đỏ
        paintNeedReview.setStyle(Paint.Style.STROKE);
        paintNeedReview.setStrokeWidth(16 * d);
        paintNeedReview.setStrokeCap(Paint.Cap.BUTT);

        paintNotStudied.setColor(0xFFE2E8F0); // xám nhạt
        paintNotStudied.setStyle(Paint.Style.STROKE);
        paintNotStudied.setStrokeWidth(16 * d);
        paintNotStudied.setStrokeCap(Paint.Cap.BUTT);

        paintCenter.setColor(0xFFFFFFFF);
        paintCenter.setStyle(Paint.Style.FILL);

        paintText.setColor(0xFF1e293b);
        paintText.setTextSize(22 * d);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        paintSubText.setColor(0xFF94a3b8);
        paintSubText.setTextSize(10 * d);
        paintSubText.setTextAlign(Paint.Align.CENTER);

        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setData(int remembered, int needReview, int notStudied) {
        this.remembered  = remembered;
        this.needReview  = needReview;
        this.notStudied  = notStudied;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float d = getResources().getDisplayMetrics().density;

        float stroke  = 16 * d;
        float padding = stroke / 2 + 4 * d;
        oval.set(padding, padding, w - padding, h - padding);

        int total = remembered + needReview + notStudied;
        boolean hasData = total > 0;

        float startAngle = -90f;
        if (!hasData) {
            // Chưa có data: vẽ vòng tròn xám đầy
            canvas.drawArc(oval, startAngle, 360f, false, paintNotStudied);
        } else {
            float sweepRemembered = 360f * remembered / total;
            float sweepNeedReview = 360f * needReview / total;
            float sweepNotStudied = 360f * notStudied / total;

            if (sweepNotStudied > 0) {
                canvas.drawArc(oval, startAngle, sweepNotStudied, false, paintNotStudied);
                startAngle += sweepNotStudied;
            }
            if (sweepNeedReview > 0) {
                canvas.drawArc(oval, startAngle, sweepNeedReview, false, paintNeedReview);
                startAngle += sweepNeedReview;
            }
            if (sweepRemembered > 0) {
                canvas.drawArc(oval, startAngle, sweepRemembered, false, paintRemembered);
            }
        }

        // Vòng tròn trắng ở giữa (tạo hiệu ứng donut)
        float cx = w / 2f;
        float cy = h / 2f;
        float innerR = (w / 2f) - stroke - 6 * d;
        canvas.drawCircle(cx, cy, innerR, paintCenter);

        // Text ở giữa
        String totalStr = hasData ? String.valueOf(total) : "0";
        float textY = cy - (paintText.descent() + paintText.ascent()) / 2;
        canvas.drawText(totalStr, cx, textY, paintText);
        canvas.drawText("words", cx, textY + 14 * d, paintSubText);
    }
}