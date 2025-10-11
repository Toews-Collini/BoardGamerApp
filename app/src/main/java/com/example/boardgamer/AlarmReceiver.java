package com.example.boardgamer;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "reminder_channel";
    private static final int NOTIF_ID = 1001;

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    @Override
    public void onReceive(Context context, Intent intent) {
        createNotificationChannel(context);

        // Intent für Button-Aktion ("Bestätigen")
        Intent confirmIntent = new Intent(context, NotificationActionReceiver.class);
        confirmIntent.setAction("CONFIRM_ACTION");
        confirmIntent.putExtra("notif_id", NOTIF_ID);
        PendingIntent confirmPendingIntent = PendingIntent.getBroadcast(
                context, 0, confirmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Notification erstellen
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.board_gamer_app_logo) // eigenes Icon
                .setContentTitle("Erinnerung")
                .setContentText("BoardGamerApp")
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Heads-up
                .setColor(Color.BLUE)
                .setAutoCancel(true)
                .addAction(R.drawable.thumbs_up, "Bestätigen", confirmPendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL); // Ton/Vibration

        NotificationManagerCompat.from(context).notify(NOTIF_ID, builder.build());
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "Erinnerungen",
                    NotificationManager.IMPORTANCE_HIGH
            );
            ch.setDescription("Zeitgesteuerte Benachrichtigungen");
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }
}
