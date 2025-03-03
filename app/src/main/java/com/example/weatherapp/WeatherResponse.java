package com.example.weatherapp;

import com.google.gson.annotations.SerializedName;

public class WeatherResponse {
    @SerializedName("name")
    public String cityName;

    @SerializedName("main")
    public Main main;

    public static class Main {
        @SerializedName("temp")
        public float temperature;
    }
}
