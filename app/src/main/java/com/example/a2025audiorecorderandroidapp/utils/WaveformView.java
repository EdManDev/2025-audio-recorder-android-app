package com.example.a2025audiorecorderandroidapp.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.example.a2025audiorecorderandroidapp.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WaveformView extends View {
    
    private Paint paint;
    private List<Float> amplitudes;
    private int maxAmplitudes = 100;
    private Random random = new Random();
    
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
        paint.setColor(ContextCompat.getColor(getContext(), android.R.color.holo_blue_bright));
        paint.setStrokeWidth(4f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        amplitudes = new ArrayList<>();
    }
    
    public void addAmplitude(float amplitude) {
        amplitudes.add(amplitude);
        if (amplitudes.size() > maxAmplitudes) {
            amplitudes.remove(0);
        }
        invalidate();
    }
    
    public void simulateAmplitude() {
        float amplitude = random.nextFloat() * 0.8f + 0.2f;
        addAmplitude(amplitude);
    }
    
    public void clear() {
        amplitudes.clear();
        invalidate();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (amplitudes.isEmpty()) return;
        
        float width = getWidth();
        float height = getHeight();
        float centerY = height / 2f;
        float barWidth = width / maxAmplitudes;
        
        for (int i = 0; i < amplitudes.size(); i++) {
            float x = i * barWidth + barWidth / 2f;
            float amplitude = amplitudes.get(i);
            float barHeight = amplitude * centerY * 0.8f;
            
            canvas.drawLine(x, centerY - barHeight, x, centerY + barHeight, paint);
        }
    }
}