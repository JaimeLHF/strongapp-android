package br.com.strongapp.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import br.com.strongapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Gráfico de linha do progresso semanal (0 a 100%), desenhado no Canvas.
 *
 * <p>É uma série só, então não há legenda: o título acima do gráfico já a nomeia.
 * Grade e eixos ficam discretos; apenas o último ponto recebe rótulo, para o gráfico
 * não virar uma lista de números.
 */
public class LineChartView extends View {

    /** Um ponto do gráfico: o rótulo do eixo X e o percentual de conclusão. */
    public static class Point {
        public final String label;
        public final float value;

        public Point(String label, float value) {
            this.label = label;
            this.value = value;
        }
    }

    private static final float MAX_VALUE = 100f;

    private final List<Point> points = new ArrayList<>();

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valueTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    public LineChartView(Context context) {
        this(context, null);
    }

    public LineChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        int brand = ContextCompat.getColor(context, R.color.brand_primary);
        int muted = themeColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant);
        int surface = themeColor(context, com.google.android.material.R.attr.colorSurface);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(2));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setColor(brand);

        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(brand);

        // Anel na cor da superfície, para pontos vizinhos não se fundirem.
        dotRingPaint.setStyle(Paint.Style.STROKE);
        dotRingPaint.setStrokeWidth(dp(2));
        dotRingPaint.setColor(surface);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(dp(1));
        gridPaint.setColor((muted & 0x00FFFFFF) | 0x33000000);

        axisTextPaint.setColor(muted);
        axisTextPaint.setTextSize(sp(11));

        valueTextPaint.setColor(muted);
        valueTextPaint.setTextSize(sp(12));
        valueTextPaint.setFakeBoldText(true);
    }

    public void setPoints(List<Point> data) {
        points.clear();
        if (data != null) {
            points.addAll(data);
        }
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (points.isEmpty()) return;

        float left = dp(38);
        float right = getWidth() - dp(14);
        float top = dp(18);
        float bottom = getHeight() - dp(20);
        if (right <= left || bottom <= top) return;

        drawGrid(canvas, left, right, top, bottom);

        float step = points.size() == 1 ? 0 : (right - left) / (points.size() - 1);
        float dotRadius = dp(4.5f);

        path.reset();
        for (int i = 0; i < points.size(); i++) {
            float x = points.size() == 1 ? (left + right) / 2 : left + step * i;
            float y = valueToY(points.get(i).value, top, bottom);
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        if (points.size() > 1) {
            canvas.drawPath(path, linePaint);
        }

        for (int i = 0; i < points.size(); i++) {
            float x = points.size() == 1 ? (left + right) / 2 : left + step * i;
            float y = valueToY(points.get(i).value, top, bottom);
            canvas.drawCircle(x, y, dotRadius, dotPaint);
            canvas.drawCircle(x, y, dotRadius, dotRingPaint);
        }

        drawEdgeLabels(canvas, left, right, top, bottom, step);
    }

    private void drawGrid(Canvas canvas, float left, float right, float top, float bottom) {
        for (int value = 0; value <= 100; value += 50) {
            float y = valueToY(value, top, bottom);
            canvas.drawLine(left, y, right, y, gridPaint);
            String label = value + "%";
            canvas.drawText(label, 0, y + sp(4), axisTextPaint);
        }
    }

    /** Só as pontas do eixo X e o valor do último ponto recebem rótulo. */
    private void drawEdgeLabels(Canvas canvas, float left, float right, float top, float bottom, float step) {
        float baseline = bottom + sp(14);
        canvas.drawText(points.get(0).label, left, baseline, axisTextPaint);

        if (points.size() > 1) {
            Point last = points.get(points.size() - 1);
            float labelWidth = axisTextPaint.measureText(last.label);
            canvas.drawText(last.label, right - labelWidth, baseline, axisTextPaint);

            String value = String.format(Locale.getDefault(), "%.0f%%", last.value);
            float valueWidth = valueTextPaint.measureText(value);
            float y = valueToY(last.value, top, bottom) - dp(10);
            canvas.drawText(value, right - valueWidth, Math.max(y, sp(12)), valueTextPaint);
        }
    }

    private static float valueToY(float value, float top, float bottom) {
        float clamped = Math.max(0, Math.min(MAX_VALUE, value));
        return bottom - (clamped / MAX_VALUE) * (bottom - top);
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }

    private static int themeColor(Context context, int attr) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attr, value, true);
        return value.data;
    }
}
