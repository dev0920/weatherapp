package com.example.weatherapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class WeatherAlertReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "WeatherAlertsChannel";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.hasExtra("temperature")) {
            float temperature = intent.getFloatExtra("temperature", 0);

            String alertMessage = null;
            if (temperature > 35) {
                alertMessage = "🔥 Heat Alert! Temperature: " + temperature + "°C";
            } else if (temperature < 5) {
                alertMessage = "❄️ Cold Alert! Temperature: " + temperature + "°C";
            }

            if (alertMessage != null) {
                showNotification(context, alertMessage);
            }
        }
    }

    private void showNotification(Context context, String message) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create Notification Channel (For Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Weather Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        // Build the Notification
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Weather Alert")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();

        notificationManager.notify(1, notification);
    }
}
