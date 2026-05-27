package com.suiyuan.iragent_app.ui.screens.profile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CoverageRingView extends View {

    private float percentage = 0f;
    private final Paint ringPaint;
    private final Paint bgPaint;
    private final Paint textPaint;
    private final Paint labelPaint;
    private final RectF arcRect = new RectF();

    public CoverageRingView(Context context) {
        this(context, null);
    }

    public CoverageRingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(dp2px(8));
        ringPaint.setColor(Color.parseColor("#6366F1"));
        ringPaint.setStrokeCap(Paint.Cap.ROUND);

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setStrokeWidth(dp2px(8));
        bgPaint.setColor(Color.parseColor("#E5E7EB"));
        bgPaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(dp2px(18));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setColor(Color.parseColor("#6366F1"));

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTextSize(dp2px(10));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setColor(Color.parseColor("#9CA3AF"));
    }

    public void setPercentage(float pct) {
        this.percentage = pct;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        float stroke = dp2px(8);
        float padding = stroke + dp2px(4);
        arcRect.set(padding, padding, w - padding, h - padding);

        // Background ring
        canvas.drawArc(arcRect, -90, 360, false, bgPaint);

        // Progress ring
        float sweepAngle = 360 * percentage;
        canvas.drawArc(arcRect, -90, sweepAngle, false, ringPaint);

        // Center text
        float cx = w / 2f;
        float cy = h / 2f;
        canvas.drawText((int)(percentage * 100) + "%", cx, cy + dp2px(4), textPaint);
        canvas.drawText("考点覆盖", cx, cy + dp2px(20), labelPaint);
    }

    private float dp2px(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
