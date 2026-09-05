package br.com.strongapp.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lembretes de treino (RF15). Guarda em preferências os dias da semana e o horário
 * escolhidos para cada treino e agenda um alarme semanal por dia marcado.
 *
 * <p>Usa alarme inexato: o lembrete pode atrasar alguns minutos, mas não exige a
 * permissão de alarme exato do Android 12+.
 */
public final class Reminders {

    private static final String PREFS = "strongapp_reminders";
    private static final String KEY_DAYS = "days_";
    private static final String KEY_HOUR = "hour_";
    private static final String KEY_MINUTE = "minute_";
    private static final String KEY_TITLE = "title_";
    private static final String KEY_IDS = "workout_ids";

    public static final String EXTRA_WORKOUT_ID = "workout_id";
    public static final String EXTRA_TITLE = "workout_title";

    private Reminders() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Dias marcados, em {@link Calendar#DAY_OF_WEEK} (1 = domingo). */
    public static Set<Integer> days(Context context, String workoutId) {
        Set<Integer> days = new HashSet<>();
        for (String value : prefs(context).getStringSet(KEY_DAYS + workoutId, new HashSet<>())) {
            days.add(Integer.parseInt(value));
        }
        return days;
    }

    public static int hour(Context context, String workoutId) {
        return prefs(context).getInt(KEY_HOUR + workoutId, 18);
    }

    public static int minute(Context context, String workoutId) {
        return prefs(context).getInt(KEY_MINUTE + workoutId, 0);
    }

    /** Grava a escolha e reagenda os alarmes desse treino. */
    public static void save(Context context, String workoutId, String title,
                            Set<Integer> days, int hour, int minute) {
        cancel(context, workoutId);

        Set<String> raw = new HashSet<>();
        for (Integer day : days) {
            raw.add(String.valueOf(day));
        }

        Set<String> ids = new HashSet<>(prefs(context).getStringSet(KEY_IDS, new HashSet<>()));
        if (days.isEmpty()) {
            ids.remove(workoutId);
        } else {
            ids.add(workoutId);
        }

        prefs(context).edit()
                .putStringSet(KEY_DAYS + workoutId, raw)
                .putInt(KEY_HOUR + workoutId, hour)
                .putInt(KEY_MINUTE + workoutId, minute)
                .putString(KEY_TITLE + workoutId, title)
                .putStringSet(KEY_IDS, ids)
                .apply();

        schedule(context, workoutId);
    }

    /** Reagenda tudo — usado depois do boot do aparelho. */
    public static void scheduleAll(Context context) {
        for (String workoutId : prefs(context).getStringSet(KEY_IDS, new HashSet<>())) {
            schedule(context, workoutId);
        }
    }

    private static void schedule(Context context, String workoutId) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;

        String title = prefs(context).getString(KEY_TITLE + workoutId, "");
        int hour = hour(context, workoutId);
        int minute = minute(context, workoutId);

        for (Integer day : days(context, workoutId)) {
            Calendar when = Calendar.getInstance();
            when.set(Calendar.DAY_OF_WEEK, day);
            when.set(Calendar.HOUR_OF_DAY, hour);
            when.set(Calendar.MINUTE, minute);
            when.set(Calendar.SECOND, 0);
            when.set(Calendar.MILLISECOND, 0);
            if (when.getTimeInMillis() <= System.currentTimeMillis()) {
                when.add(Calendar.WEEK_OF_YEAR, 1);
            }

            alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, when.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY * 7, pendingIntent(context, workoutId, title, day));
        }
    }

    public static void cancel(Context context, String workoutId) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;

        String title = prefs(context).getString(KEY_TITLE + workoutId, "");
        for (int day = Calendar.SUNDAY; day <= Calendar.SATURDAY; day++) {
            alarms.cancel(pendingIntent(context, workoutId, title, day));
        }
    }

    private static PendingIntent pendingIntent(Context context, String workoutId, String title, int day) {
        Intent intent = new Intent(context, ReminderReceiver.class)
                .putExtra(EXTRA_WORKOUT_ID, workoutId)
                .putExtra(EXTRA_TITLE, title);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, requestCode(workoutId, day), intent, flags);
    }

    /** Um código por treino e dia, para os alarmes não se sobrescreverem. */
    private static int requestCode(String workoutId, int day) {
        return (workoutId.hashCode() & 0x0FFFFFF0) | (day & 0x0F);
    }

    /** Rótulo dos dias marcados, ex.: "Seg, Qua · 18:00". */
    public static String summary(Context context, String workoutId) {
        Set<Integer> days = days(context, workoutId);
        if (days.isEmpty()) return "";

        String[] labels = {"", "Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"};
        List<String> parts = new ArrayList<>();
        for (int day = Calendar.SUNDAY; day <= Calendar.SATURDAY; day++) {
            if (days.contains(day)) {
                parts.add(labels[day]);
            }
        }
        StringBuilder joined = new StringBuilder();
        for (String part : parts) {
            if (joined.length() > 0) joined.append(", ");
            joined.append(part);
        }
        return String.format(Locale.getDefault(), "%s · %02d:%02d",
                joined, hour(context, workoutId), minute(context, workoutId));
    }
}
