package com.minimaltv.home;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * iOS 18 frosted material without a live blur: the panel shows the pre-blurred wallpaper at the panel's own
 * screen position, under a translucent tint and a one-pixel light inner edge. Matte: no gloss, no highlight.
 * The blurred bitmap is drawn through a shader whose matrix maps screen space to bitmap space, so every panel
 * reads the right patch of the photo. Call {@link #setScreenOffset} whenever the host view's position changes.
 */
final class FrostDrawable extends Drawable {

    private final Paint blurPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint tintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix = new Matrix();
    private final RectF box = new RectF();
    private final float radius;
    private final float blurScale;     // screen px per blurred-bitmap px
    private final float edgeWidth;
    private float screenX, screenY;

    /**
     * @param blurred   the blurred wallpaper (any size; stretched to the screen)
     * @param screenW   screen width in px, screenH height
     * @param radius    corner radius in px
     * @param tint      ARGB fill over the blur (the material's colour and opacity)
     * @param edge      ARGB colour of the 1px inner edge
     */
    FrostDrawable(Bitmap blurred, int screenW, int screenH, float radius, int tint, int edge, float density) {
        this.radius = radius;
        this.edgeWidth = Math.max(1f, density);
        if (blurred != null) {
            blurPaint.setShader(new BitmapShader(blurred, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
            blurScale = (float) screenW / blurred.getWidth();
        } else {
            blurScale = 1f;
        }
        tintPaint.setColor(tint);
        edgePaint.setColor(edge);
        edgePaint.setStyle(Paint.Style.STROKE);
        edgePaint.setStrokeWidth(edgeWidth);
    }

    /** Where this drawable's bounds origin sits on screen. */
    void setScreenOffset(float x, float y) {
        if (x == screenX && y == screenY) return;
        screenX = x;
        screenY = y;
        invalidateSelf();
    }

    void setFill(int tint) {
        tintPaint.setColor(tint);
        invalidateSelf();
    }

    void setEdge(int edge) {
        edgePaint.setColor(edge);
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        box.set(b);
        if (blurPaint.getShader() != null) {
            // Bitmap space -> screen space, then shift so the patch under this panel lands at the panel.
            matrix.setScale(blurScale, blurScale);
            matrix.postTranslate(-screenX, -screenY);
            blurPaint.getShader().setLocalMatrix(matrix);
            canvas.drawRoundRect(box, radius, radius, blurPaint);
        }
        canvas.drawRoundRect(box, radius, radius, tintPaint);
        float inset = edgeWidth / 2f;
        box.inset(inset, inset);
        canvas.drawRoundRect(box, radius - inset, radius - inset, edgePaint);
    }

    @Override
    public void setAlpha(int alpha) {
        // Opacity is baked into the tint; the material never fades.
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
