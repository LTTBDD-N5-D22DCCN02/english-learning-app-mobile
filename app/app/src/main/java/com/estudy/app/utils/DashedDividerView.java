package com.estudy.app.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Custom View vẽ đường nét đứt — hỗ trợ cả ngang và dọc.
 *
 * Dùng trong XML:
 *   <com.estudy.app.utils.DashedDividerView
 *       android:layout_width="1dp"
 *       android:layout_height="match_parent" />   ← divider dọc
 *
 *   <com.estudy.app.utils.DashedDividerView
 *       android:layout_width="match_parent"
 *       android:layout_height="1dp" />            ← divider ngang
 */
public class DashedDividerView extends View {

    private final Paint paint = new Paint();

    public DashedDividerView(Context context) {
        super(context);
        init();
    }

    public DashedDividerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DashedDividerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint.setColor(0xFFCBD5E1);      // #CBD5E1
        paint.setStrokeWidth(3f);        // ~1dp * density — sẽ scale theo dp bên dưới
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        // Hiệu ứng nét đứt: 5dp dash, 4dp gap
        float density = getResources().getDisplayMetrics().density;
        float dash = 5f * density;
        float gap  = 4f * density;
        paint.setPathEffect(new DashPathEffect(new float[]{dash, gap}, 0));
        paint.setStrokeWidth(1f * density);

        // Bắt buộc để DashPathEffect render được
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();

        if (w > h) {
            // Nằm ngang
            float y = h / 2f;
            canvas.drawLine(0, y, w, y, paint);
        } else {
            // Nằm dọc
            float x = w / 2f;
            canvas.drawLine(x, 0, x, h, paint);
        }
    }
}