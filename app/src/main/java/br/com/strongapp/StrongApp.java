package br.com.strongapp;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import br.com.strongapp.util.ReminderReceiver;
import br.com.strongapp.util.ThemeMode;

/** Aplica o tema salvo e cria o canal de notificação antes de qualquer tela abrir. */
public class StrongApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemeMode.apply(this);
        createReminderChannel();
    }

    private void createReminderChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationChannel channel = new NotificationChannel(
                ReminderReceiver.CHANNEL_ID,
                getString(R.string.reminder_channel),
                NotificationManager.IMPORTANCE_DEFAULT);

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
