package com.minimaltv.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/**
 * Pure black outside a rounded rectangle the size of the screen, so a square TV panel looks like it has
 * rounded corners. Drawn last, over everything, and never focusable.
 */
public final class CornerMask extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float radius;

    public CornerMask(Context context, float radiusPx) {
        super(context);
        radius = radiusPx;
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        setWillNotDraw(false);
        setFocusable(false);
        setClickable(false);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        path.reset();
        path.addRect(0, 0, w, h, Path.Direction.CW);
        path.addRoundRect(new RectF(0, 0, w, h), radius, radius, Path.Direction.CCW);
        path.setFillType(Path.FillType.EVEN_ODD);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawPath(path, paint);
    }
}
