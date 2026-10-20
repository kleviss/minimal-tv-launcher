package com.minimaltv.home;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * A vertical lift in one colour whose opacity eases in with a smoothstep over the top part of the bounds and
 * then holds. Sampled at many stops so the ease is a curve, not a visible fold.
 */
final class LiftDrawable extends Drawable {

    private final Paint paint = new Paint();
    private final int rgb;
    private final int maxAlpha;
    private final float easeFraction;   // part of the height over which the lift eases in

    LiftDrawable(int rgb, int maxAlpha, float easeFraction) {
        this.rgb = rgb & 0x00FFFFFF;
        this.maxAlpha = maxAlpha;
        this.easeFraction = easeFraction;
        paint.setDither(true);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        int n = 32;
        int[] colors = new int[n];
        float[] positions = new float[n];
        for (int i = 0; i < n; i++) {
            float pos = i / (float) (n - 1);
            float t = Math.min(1f, pos / easeFraction);
            float eased = t * t * (3 - 2 * t);
            colors[i] = (Math.round(maxAlpha * eased) << 24) | rgb;
            positions[i] = pos;
        }
        paint.setShader(new LinearGradient(0, bounds.top, 0, bounds.bottom, colors, positions, Shader.TileMode.CLAMP));
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawRect(getBounds(), paint);
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
