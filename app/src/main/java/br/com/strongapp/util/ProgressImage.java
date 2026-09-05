package br.com.strongapp.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

/**
 * Imagem de progresso no formato 9:16 para compartilhar (RF13), com os mesmos
 * textos da versão web: marca da academia, treino, percentual e a hashtag.
 */
public final class ProgressImage {

    private static final int WIDTH = 1080;
    private static final int HEIGHT = 1920;

    private static final int BACKGROUND = Color.parseColor("#0B1120");
    private static final int SURFACE = Color.parseColor("#111827");
    private static final int BRAND = Color.parseColor("#F4511F");
    private static final int TEXT = Color.parseColor("#F9FAFB");
    private static final int MUTED = Color.parseColor("#9CA3AF");

    private ProgressImage() {
    }

    /**
     * Gera o PNG em {@code cache/exports} e devolve o arquivo.
     *
     * @param workoutTitle nome do treino
     * @param done         exercícios concluídos na semana
     * @param total        exercícios do treino
     * @param week         número da semana ISO
     */
    public static File create(Context context, String workoutTitle, int done, int total, int week)
            throws IOException {
        int percentage = total == 0 ? 0 : Math.round(done * 100f / total);

        Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(BACKGROUND);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextAlign(Paint.Align.CENTER);

        paint.setColor(BRAND);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(56);
        canvas.drawText("💪 RITMO FORTE GYM", WIDTH / 2f, 220, paint);

        paint.setColor(SURFACE);
        canvas.drawRoundRect(new RectF(90, 380, WIDTH - 90, 1420), 48, 48, paint);

        paint.setColor(TEXT);
        paint.setTextSize(64);
        drawWrapped(canvas, paint, workoutTitle, WIDTH / 2f, 540, WIDTH - 260);

        // Anel de progresso: o arco laranja sobre a trilha cinza.
        RectF ring = new RectF(WIDTH / 2f - 260, 700, WIDTH / 2f + 260, 1220);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(46);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(Color.parseColor("#1F2937"));
        canvas.drawArc(ring, 0, 360, false, paint);
        paint.setColor(BRAND);
        canvas.drawArc(ring, -90, 360f * percentage / 100f, false, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(BRAND);
        paint.setTextSize(190);
        canvas.drawText(percentage + "%", WIDTH / 2f, 1025, paint);

        // Legendas abaixo do anel, para não cruzarem o arco.
        paint.setColor(MUTED);
        paint.setTextSize(46);
        canvas.drawText(String.format(Locale.getDefault(), "%d de %d exercícios", done, total),
                WIDTH / 2f, 1305, paint);
        canvas.drawText(String.format(Locale.getDefault(), "Semana %d", week), WIDTH / 2f, 1375, paint);

        paint.setColor(BRAND);
        paint.setTextSize(52);
        canvas.drawText("#RitmoForteGym", WIDTH / 2f, 1650, paint);

        paint.setColor(MUTED);
        paint.setTextSize(40);
        canvas.drawText("StrongApp", WIDTH / 2f, 1740, paint);

        File dir = new File(context.getCacheDir(), "exports");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Não foi possível criar a pasta de exportação.");
        }
        File file = new File(dir, "progresso-strongapp.png");
        try (OutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        bitmap.recycle();
        return file;
    }

    /** Quebra o título em até duas linhas para caber na largura do cartão. */
    private static void drawWrapped(Canvas canvas, Paint paint, String text, float x, float y, float maxWidth) {
        if (text == null) text = "";
        if (paint.measureText(text) <= maxWidth) {
            canvas.drawText(text, x, y, paint);
            return;
        }

        String[] words = text.split(" ");
        StringBuilder first = new StringBuilder();
        int i = 0;
        while (i < words.length && paint.measureText(first + words[i] + " ") <= maxWidth) {
            first.append(words[i]).append(' ');
            i++;
        }
        StringBuilder second = new StringBuilder();
        while (i < words.length) {
            second.append(words[i]).append(' ');
            i++;
        }
        canvas.drawText(first.toString().trim(), x, y - 36, paint);
        canvas.drawText(second.toString().trim(), x, y + 36, paint);
    }
}
