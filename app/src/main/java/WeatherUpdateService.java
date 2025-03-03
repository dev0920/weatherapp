package com.example.weatherapp;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WeatherUpdateService extends Service {

    private static final String TAG = "WeatherUpdateService";
    private static final String CHANNEL_ID = "WeatherUpdatesChannel";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    private static final String API_KEY = "89c993fb3cc1ab5290c4485b553886f3";  // Use Gradle method if needed
    private static final String CITY = "Mumbai";  // Default city for updates
    private Handler handler;
    private Runnable weatherUpdater;
    private Retrofit retrofit;
    private WeatherAPI weatherAPI;

    @SuppressLint("ForegroundServiceType")
    @Override
    public void onCreate() {
        super.onCreate();

        // Create notification channel (for Android 8+)
        createNotificationChannel();

        // Start Foreground Service with Notification
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Weather Updates Running")
                .setContentText("Fetching latest weather updates...")
                .setSmallIcon(R.drawable.ic_launcher_background)  // Replace with your icon
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(1, notification);

        // Initialize Retrofit
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        weatherAPI = retrofit.create(WeatherAPI.class);

        handler = new Handler();
        weatherUpdater = new Runnable() {
            @Override
            public void run() {
                fetchWeather(CITY);
                handler.postDelayed(this, 30 * 60 * 1000);  // Fetch every 30 minutes
            }
        };
        handler.post(weatherUpdater);  // Start periodic updates
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Weather Updates",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void fetchWeather(String city) {
        Call<WeatherResponse> call = weatherAPI.getWeather(city, API_KEY, "metric");

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse weather = response.body();
                    float temp = weather.main.temperature;
                    Log.d(TAG, "Updated Weather: " + city + " - " + temp + "°C");

                    // Update notification with latest weather
                    updateNotification("Current temp in " + city + ": " + temp + "°C");

                    // Send Broadcast for weather alert
                    Intent intent = new Intent(WeatherUpdateService.this, WeatherAlertReceiver.class);
                    intent.putExtra("temperature", temp);
                    sendBroadcast(intent);
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Log.e(TAG, "Failed to fetch weather data", t);
            }
        });
    }

    @SuppressLint("ForegroundServiceType")
    private void updateNotification(String text) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Weather Updates Running")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;  // Ensures service restarts if killed
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(weatherUpdater);  // Stop updates when service is destroyed
    }
}
