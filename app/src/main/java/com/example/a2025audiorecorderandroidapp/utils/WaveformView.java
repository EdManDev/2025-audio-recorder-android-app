package com.example.a2025audiorecorderandroidapp.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.example.a2025audiorecorderandroidapp.R;
import java.util.ArrayList;
import java.util.List;

public class WaveformView extends View {

    public enum WaveformStyle {
        BARS, LINE, CIRCULAR
    }

    private Paint paint;
    private Paint linePaint;
    private List<Float> amplitudes;
    private int maxAmplitudes = 100;
    private WaveformStyle style = WaveformStyle.BARS;
    private Path path = new Path();

    public WaveformView(Context context) {
        super(context);
        init();
    }

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WaveformView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(ContextCompat.getColor(getContext(), R.color.waveform_blue));
        paint.setStrokeWidth(4f);
        paint.setStrokeCap(Paint.Cap.ROUND);

        linePaint = new Paint();
        linePaint.setColor(ContextCompat.getColor(getContext(), R.color.waveform_blue));
        linePaint.setStrokeWidth(2f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);

        amplitudes = new ArrayList<>();
    }

    public void setWaveformStyle(WaveformStyle style) {
        this.style = style;
        invalidate();
    }

    public void addAmplitude(float amplitude) {
        // Normalize amplitude to 0-1 range
        amplitude = Math.max(0, Math.min(1, amplitude));

        amplitudes.add(amplitude);
        if (amplitudes.size() > maxAmplitudes) {
            amplitudes.remove(0);
        }
        invalidate();
    }

    public void addAmplitudes(float[] newAmplitudes) {
        for (float amp : newAmplitudes) {
            addAmplitude(amp);
        }
    }

    public void clear() {
        amplitudes.clear();
        invalidate();
    }

    public void simulateAmplitude() {
        float amplitude = (float) (Math.random() * 0.8f + 0.2f);
        addAmplitude(amplitude);
    }

    public void setMaxAmplitudes(int max) {
        this.maxAmplitudes = max;
        while (amplitudes.size() > maxAmplitudes) {
            amplitudes.remove(0);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (amplitudes.isEmpty()) return;

        float width = getWidth();
        float height = getHeight();

        switch (style) {
            case BARS:
                drawBars(canvas, width, height);
                break;
            case LINE:
                drawLineWave(canvas, width, height);
                break;
            case CIRCULAR:
                drawCircular(canvas, width, height);
                break;
        }
    }

    private void drawBars(Canvas canvas, float width, float height) {
        float centerY = height / 2f;
        float barWidth = width / maxAmplitudes;

        for (int i = 0; i < amplitudes.size(); i++) {
            float x = i * barWidth + barWidth / 2f;
            float amplitude = amplitudes.get(i);
            float barHeight = amplitude * centerY * 0.8f;

            canvas.drawLine(x, centerY - barHeight, x, centerY + barHeight, paint);
        }
    }

    private void drawLineWave(Canvas canvas, float width, float height) {
        if (amplitudes.size() < 2) return;

        path.reset();
        float centerY = height / 2f;
        float stepX = width / (amplitudes.size() - 1);

        path.moveTo(0, centerY - amplitudes.get(0) * centerY * 0.8f);
        for (int i = 1; i < amplitudes.size(); i++) {
            float x = i * stepX;
            float y = centerY - amplitudes.get(i) * centerY * 0.8f;
            path.lineTo(x, y);
        }

        canvas.drawPath(path, linePaint);
    }

    private void drawCircular(Canvas canvas, float width, float height) {
        float centerX = width / 2f;
        float centerY = height / 2f;
        float radius = Math.min(centerX, centerY) * 0.8f;

        if (amplitudes.isEmpty()) return;

        float angleStep = 360f / amplitudes.size();
        float currentAngle = -90; // Start from top

        path.reset();
        for (int i = 0; i < amplitudes.size(); i++) {
            float amplitude = amplitudes.get(i);
            float distance = radius * (0.3f + amplitude * 0.7f); // Min 30% to max 100% of radius

            double radian = Math.toRadians(currentAngle);
            float x = centerX + (float) (Math.cos(radian) * distance);
            float y = centerY + (float) (Math.sin(radian) * distance);

            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }

            currentAngle += angleStep;
        }
        path.close();

        canvas.drawPath(path, linePaint);
    }
}