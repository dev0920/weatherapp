package com.example.weatherapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "WeatherPrefs";
    private static final String KEY_UPDATE_INTERVAL = "update_interval";
    private Spinner spinnerUpdateInterval;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        spinnerUpdateInterval = findViewById(R.id.spinnerUpdateInterval);
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Setup dropdown values
        String[] intervals = {"15 minutes", "30 minutes", "1 hour"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, intervals);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUpdateInterval.setAdapter(adapter);

        // Load saved preference
        int savedInterval = sharedPreferences.getInt(KEY_UPDATE_INTERVAL, 30);
        spinnerUpdateInterval.setSelection(savedInterval == 15 ? 0 : savedInterval == 30 ? 1 : 2);

        // Save preference when user selects an option
        spinnerUpdateInterval.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedInterval = (position == 0) ? 15 : (position == 1) ? 30 : 60;

                sharedPreferences.edit().putInt(KEY_UPDATE_INTERVAL, selectedInterval).apply();
                Toast.makeText(SettingsActivity.this, "Update interval set to " + selectedInterval + " minutes", Toast.LENGTH_SHORT).show();

                // Restart WorkManager with new interval
                MainActivity.scheduleWeatherUpdates(SettingsActivity.this, selectedInterval);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
}
