package com.example.weatherapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import java.util.concurrent.TimeUnit;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    EditText etCity;
    Button btnGetWeather, btnSaveCity, btnSettings;
    TextView tvWeather, tvSavedCity;
    Retrofit retrofit;
    WeatherAPI weatherAPI;
    SharedPreferences sharedPreferences;

    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    private static final String API_KEY = "89c993fb3cc1ab5290c4485b553886f3";  // Use Gradle method for security
    private static final String PREFS_NAME = "WeatherPrefs";
    private static final String KEY_CITY = "saved_city";
    private static final String KEY_UPDATE_INTERVAL = "update_interval";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Ensure correct layout is set

        // Initialize Views
        etCity = findViewById(R.id.etCity);
        btnGetWeather = findViewById(R.id.btnGetWeather);
        btnSaveCity = findViewById(R.id.btnSaveCity);
        btnSettings = findViewById(R.id.btnSettings);
        tvWeather = findViewById(R.id.tvWeather);
        tvSavedCity = findViewById(R.id.tvSavedCity);

        // Ensure all views are properly initialized
        if (etCity == null || btnGetWeather == null || btnSaveCity == null || btnSettings == null ||
                tvWeather == null || tvSavedCity == null) {
            Toast.makeText(this, "Error: UI elements not found. Check layout file!", Toast.LENGTH_LONG).show();
            return;
        }

        // Request Notification Permission (For Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedCity = sharedPreferences.getString(KEY_CITY, "Mumbai");
        int updateInterval = sharedPreferences.getInt(KEY_UPDATE_INTERVAL, 30); // Default to 30 minutes

        etCity.setText(savedCity);
        tvSavedCity.setText("Saved City: " + savedCity);

        // Start WorkManager with user-selected interval
        scheduleWeatherUpdates(this, updateInterval);

        // Initialize Retrofit
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        weatherAPI = retrofit.create(WeatherAPI.class);

        btnGetWeather.setOnClickListener(v -> {
            String city = etCity.getText().toString().trim();
            if (!city.isEmpty()) {
                fetchWeather(city);
            } else {
                Toast.makeText(MainActivity.this, "Please enter a city name", Toast.LENGTH_SHORT).show();
            }
        });

        btnSaveCity.setOnClickListener(v -> {
            String city = etCity.getText().toString().trim();
            if (!city.isEmpty()) {
                saveCity(city);
            } else {
                Toast.makeText(MainActivity.this, "Please enter a city name to save", Toast.LENGTH_SHORT).show();
            }
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void saveCity(String city) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_CITY, city);
        editor.apply();
        Toast.makeText(this, "City saved: " + city, Toast.LENGTH_SHORT).show();

        // Apply fade-in animation to saved city text
        tvSavedCity.setText("Saved City: " + city);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        tvSavedCity.startAnimation(fadeIn);

        // Restart WorkManager with new city
        int updateInterval = sharedPreferences.getInt(KEY_UPDATE_INTERVAL, 30); // Get saved interval
        scheduleWeatherUpdates(this, updateInterval);
    }

    public static void scheduleWeatherUpdates(Context context, int intervalMinutes) {
        WorkRequest weatherWorkRequest =
                new PeriodicWorkRequest.Builder(WeatherWorker.class, intervalMinutes, TimeUnit.MINUTES)
                        .setInputData(
                                new androidx.work.Data.Builder()
                                        .putString("city", getSavedCity(context))
                                        .build()
                        )
                        .build();
        WorkManager.getInstance(context).enqueue(weatherWorkRequest);
    }

    private static String getSavedCity(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getString(KEY_CITY, "Mumbai");
    }

    private void fetchWeather(String city) {
        Call<WeatherResponse> call = weatherAPI.getWeather(city, API_KEY, "metric");

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse weather = response.body();
                    float temp = weather.main.temperature;

                    // Apply fade-in animation to weather info
                    tvWeather.setText("City: " + weather.cityName + "\nTemperature: " + temp + "°C");
                    Animation fadeIn = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_in);
                    tvWeather.startAnimation(fadeIn);

                    // Send Broadcast for weather alert
                    Intent intent = new Intent(MainActivity.this, WeatherAlertReceiver.class);
                    intent.putExtra("temperature", temp);
                    sendBroadcast(intent);
                } else {
                    tvWeather.setText("City not found.");
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                tvWeather.setText("Error fetching data.");
            }
        });
    }
}
