package com.example.a2025audiorecorderandroidapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class SettingsActivity extends AppCompatActivity {

    private Switch switchNotificationsEnabled;
    private Spinner spinnerNotificationSound;
    private Spinner spinnerNotificationPriority;
    private CheckBox checkBoxShowPlayAction;
    private CheckBox checkBoxShowShareAction;
    private CheckBox checkBoxShowDeleteAction;
    private CheckBox checkBoxShowOpenAppAction;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        initViews();
        setupSpinners();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        switchNotificationsEnabled = findViewById(R.id.switchNotificationsEnabled);
        spinnerNotificationSound = findViewById(R.id.spinnerNotificationSound);
        spinnerNotificationPriority = findViewById(R.id.spinnerNotificationPriority);
        checkBoxShowPlayAction = findViewById(R.id.checkBoxShowPlayAction);
        checkBoxShowShareAction = findViewById(R.id.checkBoxShowShareAction);
        checkBoxShowDeleteAction = findViewById(R.id.checkBoxShowDeleteAction);
        checkBoxShowOpenAppAction = findViewById(R.id.checkBoxShowOpenAppAction);
    }

    private void setupSpinners() {
        // Notification sound options
        ArrayAdapter<CharSequence> soundAdapter = ArrayAdapter.createFromResource(this,
            R.array.notification_sound_options, android.R.layout.simple_spinner_item);
        soundAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNotificationSound.setAdapter(soundAdapter);

        // Notification priority options
        ArrayAdapter<CharSequence> priorityAdapter = ArrayAdapter.createFromResource(this,
            R.array.notification_priority_options, android.R.layout.simple_spinner_item);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNotificationPriority.setAdapter(priorityAdapter);
    }

    private void loadSettings() {
        switchNotificationsEnabled.setChecked(preferences.getBoolean("notifications_enabled", true));
        spinnerNotificationSound.setSelection(preferences.getInt("notification_sound", 0));
        spinnerNotificationPriority.setSelection(preferences.getInt("notification_priority", 1)); // Default to normal
        checkBoxShowPlayAction.setChecked(preferences.getBoolean("show_play_action", true));
        checkBoxShowShareAction.setChecked(preferences.getBoolean("show_share_action", true));
        checkBoxShowDeleteAction.setChecked(preferences.getBoolean("show_delete_action", true));
        checkBoxShowOpenAppAction.setChecked(preferences.getBoolean("show_open_app_action", true));
    }

    private void setupListeners() {
        switchNotificationsEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting("notifications_enabled", isChecked);
        });

        spinnerNotificationSound.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                saveSetting("notification_sound", position);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        spinnerNotificationPriority.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                saveSetting("notification_priority", position);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        checkBoxShowPlayAction.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting("show_play_action", isChecked);
        });

        checkBoxShowShareAction.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting("show_share_action", isChecked);
        });

        checkBoxShowDeleteAction.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting("show_delete_action", isChecked);
        });

        checkBoxShowOpenAppAction.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting("show_open_app_action", isChecked);
        });
    }

    private void saveSetting(String key, boolean value) {
        preferences.edit().putBoolean(key, value).apply();
        Toast.makeText(this, "Setting saved", Toast.LENGTH_SHORT).show();
    }

    private void saveSetting(String key, int value) {
        preferences.edit().putInt(key, value).apply();
        Toast.makeText(this, "Setting saved", Toast.LENGTH_SHORT).show();
    }
}