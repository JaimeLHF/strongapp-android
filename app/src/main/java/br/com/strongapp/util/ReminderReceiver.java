package br.com.strongapp.util;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import br.com.strongapp.R;
import br.com.strongapp.ui.MainActivity;

/** Publica a notificação do lembrete no horário agendado (RF15). */
public class ReminderReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "treino_lembrete";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra(Reminders.EXTRA_TITLE);
        String workoutId = intent.getStringExtra(Reminders.EXTRA_WORKOUT_ID);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent open = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class), flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_workouts)
                .setContentTitle(context.getString(R.string.reminder_notification_title))
                .setContentText(context.getString(R.string.reminder_notification_text,
                        title == null ? "" : title))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(open);

        NotificationManagerCompat.from(context)
                .notify(workoutId == null ? 1 : workoutId.hashCode(), builder.build());
    }
}
