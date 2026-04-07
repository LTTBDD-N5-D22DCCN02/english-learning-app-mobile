package com.estudy.app.utils;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

/**
 * Circular donut chart displaying accuracy percentage.
 * Shows a ring (arc) with the correct % filled in green and remainder in light gray.
 */
public class AccuracyRingView extends View {

    private Paint paintRing;
    private Paint paintBg;
    private Paint paintText;
    private Paint paintSubText;

    private float accuracy = 0f;   // 0–100
    private int   correct  = 0;
    private int   total    = 0;

    private static final int COLOR_CORRECT = 0xFF4CAF50;
    private static final int COLOR_BG      = 0xFFE8F5E9;
    private static final int STROKE_WIDTH  = 28;

    public AccuracyRingView(Context context) { this(context, null); }
    public AccuracyRingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBg.setStyle(Paint.Style.STROKE);
        paintBg.setStrokeWidth(STROKE_WIDTH);
        paintBg.setColor(COLOR_BG);
        paintBg.setStrokeCap(Paint.Cap.ROUND);

        paintRing = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintRing.setStyle(Paint.Style.STROKE);
        paintRing.setStrokeWidth(STROKE_WIDTH);
        paintRing.setColor(COLOR_CORRECT);
        paintRing.setStrokeCap(Paint.Cap.ROUND);

        paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(52f);
        paintText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paintText.setColor(0xFF1E293B);

        paintSubText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintSubText.setTextAlign(Paint.Align.CENTER);
        paintSubText.setTextSize(22f);
        paintSubText.setColor(0xFF64748B);
    }

    /**
     * Set result data — triggers a redraw.
     */
    public void setResult(int correct, int total) {
        this.correct  = correct;
        this.total    = total;
        this.accuracy = total > 0 ? correct * 100f / total : 0f;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        float pad   = STROKE_WIDTH / 2f + 8f;
        float size  = Math.min(w, h) - pad * 2;
        float left  = (w - size) / 2f;
        float top   = (h - size) / 2f;
        RectF oval  = new RectF(left + pad, top + pad,
                left + size - pad, top + size - pad);

        // Background ring
        canvas.drawArc(oval, 0f, 360f, false, paintBg);

        // Foreground arc (start at top = -90°)
        float sweep = accuracy * 360f / 100f;
        canvas.drawArc(oval, -90f, sweep, false, paintRing);

        // Center text: percentage
        float cx = w / 2f;
        float cy = h / 2f;
        String pct = String.format("%.0f%%", accuracy);
        float textY = cy - (paintText.descent() + paintText.ascent()) / 2f;
        canvas.drawText(pct, cx, textY, paintText);

        // Sub-text: correct/total
        String sub = correct + " / " + total;
        float subY = textY + paintText.getTextSize() * 0.55f + paintSubText.getTextSize();
        canvas.drawText(sub, cx, subY, paintSubText);
    }
}
