package com.hk.hkterminal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/**
 * HK-OPERATION : ELITE VISUAL ENGINE
 * IDENTITY     : HK Prashant Singh (Tech Wizard)
 * DIRECTIVE    : Radioactive UI & Lag-Free Matrix Renderer
 */
public class TerminalView extends View {
    private Paint textPaint;
    private Paint backgroundPaint;
    private List<String> textMatrix;
    private int maxLines = 2000; // Deep buffer for heavy logs (like nmap/tcpdump)
    private float charHeight;
    private float charWidth;

    public TerminalView(Context context) {
        super(context);
        igniteVisualEngine();
    }

    public TerminalView(Context context, AttributeSet attrs) {
        super(context, attrs);
        igniteVisualEngine();
    }

    // Initialize the Tech Wizard Matrix Graphics
    private void igniteVisualEngine() {
        textMatrix = new ArrayList<>();
        
        // Stealth Background - Abyss Black
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#050505"));

        // Cyber Weapon Text - Neon Green with Anti-Alias
        textPaint = new Paint();
        textPaint.setColor(Color.parseColor("#00FF41")); // Pure Matrix Green
        textPaint.setTextSize(40f); // Aggressive and readable size
        textPaint.setTypeface(Typeface.MONOSPACE);
        textPaint.setAntiAlias(true);
        
        // The Lethal Touch: Neon Shadow Glow Effect
        textPaint.setShadowLayer(8f, 0f, 0f, Color.parseColor("#00FF41"));

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        charHeight = fm.descent - fm.ascent;
        charWidth = textPaint.measureText("M");

        // Initial Boot Sequence Text
        appendMatrix(">> HK Prashant Bhai");
        appendMatrix("pshacker@hk:~$ ");
    }

    // Direct Injection of output to the Matrix Array
    public void appendMatrix(String rawText) {
        if (rawText == null) return;
        
        // Basic parser for new lines
        String[] lines = rawText.split("\\n");
        for (String line : lines) {
            textMatrix.add(line);
            
            // Auto-clean memory if buffer gets too heavy
            if (textMatrix.size() > maxLines) {
                textMatrix.remove(0);
            }
        }
        
        // Force hardware to redraw the screen instantly
        postInvalidate(); 
    }

    // Hardware Accelerated Canvas Drawing
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // 1. Paint the Abyss
        canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);

        // 2. Calculate visible lines (Auto-Scroll Logic)
        float y = charHeight;
        int maxVisibleLines = (int) (getHeight() / charHeight);
        int startLine = Math.max(0, textMatrix.size() - maxVisibleLines);

        // 3. Render the Neon Text
        for (int i = startLine; i < textMatrix.size(); i++) {
            // X=15f for a slight clean margin from the left edge
            canvas.drawText(textMatrix.get(i), 15f, y, textPaint);
            y += charHeight;
        }
    }

    // Getters for bridge mapping
    public int getVisibleRows() {
        return (int) (getHeight() / charHeight);
    }

    public int getVisibleColumns() {
        return (int) (getWidth() / charWidth);
    }
}
