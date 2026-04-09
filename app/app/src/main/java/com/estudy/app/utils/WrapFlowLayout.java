package com.estudy.app.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Layout tự động xuống dòng khi hết chiều ngang — dùng cho letter boxes.
 */
public class WrapFlowLayout extends ViewGroup {

    private int hGap = 8;  // gap ngang (px)
    private int vGap = 8;  // gap dọc (px)

    public WrapFlowLayout(Context context) { super(context); }
    public WrapFlowLayout(Context context, AttributeSet attrs) { super(context, attrs); }
    public WrapFlowLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setGap(int hPx, int vPx) { hGap = hPx; vGap = vPx; }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int x = 0, y = 0, rowHeight = 0, totalHeight = 0;

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            int cw = child.getMeasuredWidth();
            int ch = child.getMeasuredHeight();

            if (x + cw > width && x > 0) {
                // Xuống dòng
                totalHeight += rowHeight + vGap;
                x = 0;
                rowHeight = 0;
            }
            x += cw + hGap;
            rowHeight = Math.max(rowHeight, ch);
        }
        totalHeight += rowHeight + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),
                resolveSize(totalHeight, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width     = r - l - getPaddingLeft() - getPaddingRight();
        int padLeft   = getPaddingLeft();
        int y         = getPaddingTop();

        // ── Chia children thành từng hàng ────────────────────────
        List<List<View>> rows    = new ArrayList<>();
        List<Integer>    rowWidths = new ArrayList<>();

        List<View> currentRow  = new ArrayList<>();
        int        currentW    = 0;
        int        rowHeight   = 0;

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            int cw = child.getMeasuredWidth();

            // Xuống dòng nếu không còn chỗ
            if (!currentRow.isEmpty() && currentW + cw > width) {
                rows.add(currentRow);
                rowWidths.add(currentW - hGap); // trừ gap cuối
                currentRow = new ArrayList<>();
                currentW   = 0;
            }
            currentRow.add(child);
            currentW += cw + hGap;
        }
        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
            rowWidths.add(currentW - hGap);
        }

        // ── Vẽ từng hàng, offset để căn giữa ─────────────────────
        for (int row = 0; row < rows.size(); row++) {
            List<View> rowViews = rows.get(row);
            int        rw       = rowWidths.get(row);
            int        x        = padLeft + (width - rw) / 2; // offset giữa
            int        maxH     = 0;

            for (View child : rowViews) {
                int cw = child.getMeasuredWidth();
                int ch = child.getMeasuredHeight();
                child.layout(x, y, x + cw, y + ch);
                x   += cw + hGap;
                maxH = Math.max(maxH, ch);
            }
            y += maxH + vGap;
        }
    }
}