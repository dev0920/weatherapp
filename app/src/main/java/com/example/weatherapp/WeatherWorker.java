package com.example.weatherapp;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WeatherWorker extends Worker {

    private static final String TAG = "WeatherWorker";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    private static final String API_KEY = "89c993fb3cc1ab5290c4485b553886f3";  // Use Gradle method if needed
    private WeatherAPI weatherAPI;

    public WeatherWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        weatherAPI = retrofit.create(WeatherAPI.class);
    }

    @NonNull
    @Override
    public Result doWork() {
        String city = getInputData().getString("city");  // Default to Mumbai
        fetchWeather(city);
        return Result.success();  // Work completed successfully
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

                    // Send Broadcast for weather alert
                    Intent intent = new Intent(getApplicationContext(), WeatherAlertReceiver.class);
                    intent.putExtra("temperature", temp);
                    getApplicationContext().sendBroadcast(intent);
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Log.e(TAG, "Failed to fetch weather data", t);
            }
        });
    }
}
