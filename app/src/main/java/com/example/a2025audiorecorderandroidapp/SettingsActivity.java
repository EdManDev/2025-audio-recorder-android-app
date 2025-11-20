package com.example.a2025audiorecorderandroidapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.SeekBar;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat switchNotificationsEnabled;
    private LinearLayout layoutNotificationSound;
    private TextView textViewCurrentNotificationSound;
    private LinearLayout layoutNotificationPriority;
    private TextView textViewCurrentNotificationPriority;
    private CheckBox checkBoxShowPlayAction;
    private CheckBox checkBoxShowShareAction;
    private CheckBox checkBoxShowDeleteAction;
    private CheckBox checkBoxShowOpenAppAction;
    private LinearLayout layoutAudioFormat;
    private TextView textViewCurrentAudioFormat;
    private LinearLayout layoutRecordingsFolder;
    private TextView textViewCurrentRecordingsFolder;
    private LinearLayout layoutRecordingSource;
    private TextView textViewCurrentRecordingSource;
    private LinearLayout layoutSampleRate;
    private TextView textViewCurrentSampleRate;
    private LinearLayout layoutEncoding;
    private TextView textViewCurrentEncoding;
    private LinearLayout layoutAudioChannels;
    private TextView textViewCurrentAudioChannels;
    private LinearLayout layoutFilenameFormat;
    private TextView textViewCurrentFilenameFormat;
    private SwitchCompat switchVoiceFilter;
    private LinearLayout layoutRecordingVolume;
    private TextView textViewCurrentRecordingVolume;
    private SwitchCompat switchSkipSilence;
    private SwitchCompat switchEncodingOnTheFly;
    private SwitchCompat switchPauseDuringCall;
    private SwitchCompat switchSilentMode;
    private SwitchCompat switchLockscreenControls;
    private LinearLayout layoutApplicationTheme;
    private TextView textViewCurrentApplicationTheme;

    private SharedPreferences preferences;
    private ActivityResultLauncher<Intent> folderPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        folderPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            String path = uri.getPath();
                            if (path != null) {
                                saveSetting("recordings_folder", path);
                                textViewCurrentRecordingsFolder.setText(path);
                            }
                        }
                    }
                });

        initViews();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        switchNotificationsEnabled = findViewById(R.id.switchNotificationsEnabled);
        layoutNotificationSound = findViewById(R.id.layoutNotificationSound);
        textViewCurrentNotificationSound = findViewById(R.id.textViewCurrentNotificationSound);
        layoutNotificationPriority = findViewById(R.id.layoutNotificationPriority);
        textViewCurrentNotificationPriority = findViewById(R.id.textViewCurrentNotificationPriority);
        checkBoxShowPlayAction = findViewById(R.id.checkBoxShowPlayAction);
        checkBoxShowShareAction = findViewById(R.id.checkBoxShowShareAction);
        checkBoxShowDeleteAction = findViewById(R.id.checkBoxShowDeleteAction);
        checkBoxShowOpenAppAction = findViewById(R.id.checkBoxShowOpenAppAction);
        layoutAudioFormat = findViewById(R.id.layoutAudioFormat);
        textViewCurrentAudioFormat = findViewById(R.id.textViewCurrentAudioFormat);
        layoutRecordingsFolder = findViewById(R.id.layoutRecordingsFolder);
        textViewCurrentRecordingsFolder = findViewById(R.id.textViewCurrentRecordingsFolder);
        layoutRecordingSource = findViewById(R.id.layoutRecordingSource);
        textViewCurrentRecordingSource = findViewById(R.id.textViewCurrentRecordingSource);
        layoutSampleRate = findViewById(R.id.layoutSampleRate);
        textViewCurrentSampleRate = findViewById(R.id.textViewCurrentSampleRate);
        layoutEncoding = findViewById(R.id.layoutEncoding);
        textViewCurrentEncoding = findViewById(R.id.textViewCurrentEncoding);
        layoutAudioChannels = findViewById(R.id.layoutAudioChannels);
        textViewCurrentAudioChannels = findViewById(R.id.textViewCurrentAudioChannels);
        layoutFilenameFormat = findViewById(R.id.layoutFilenameFormat);
        textViewCurrentFilenameFormat = findViewById(R.id.textViewCurrentFilenameFormat);
        switchVoiceFilter = findViewById(R.id.switchVoiceFilter);
        layoutRecordingVolume = findViewById(R.id.layoutRecordingVolume);
        textViewCurrentRecordingVolume = findViewById(R.id.textViewCurrentRecordingVolume);
        switchSkipSilence = findViewById(R.id.switchSkipSilence);
        switchEncodingOnTheFly = findViewById(R.id.switchEncodingOnTheFly);
        switchPauseDuringCall = findViewById(R.id.switchPauseDuringCall);
        switchSilentMode = findViewById(R.id.switchSilentMode);
        switchLockscreenControls = findViewById(R.id.switchLockscreenControls);
        layoutApplicationTheme = findViewById(R.id.layoutApplicationTheme);
        textViewCurrentApplicationTheme = findViewById(R.id.textViewCurrentApplicationTheme);
    }



    private void loadSettings() {
        switchNotificationsEnabled.setChecked(preferences.getBoolean("notifications_enabled", true));
        int notificationSoundIndex = preferences.getInt("notification_sound", 0);
        String[] notificationSounds = getResources().getStringArray(R.array.notification_sound_options);
        textViewCurrentNotificationSound.setText(notificationSounds[notificationSoundIndex]);
        int notificationPriorityIndex = preferences.getInt("notification_priority", 1); // Default to normal
        String[] notificationPriorities = getResources().getStringArray(R.array.notification_priority_options);
        textViewCurrentNotificationPriority.setText(notificationPriorities[notificationPriorityIndex]);
        checkBoxShowPlayAction.setChecked(preferences.getBoolean("show_play_action", true));
        checkBoxShowShareAction.setChecked(preferences.getBoolean("show_share_action", true));
        checkBoxShowDeleteAction.setChecked(preferences.getBoolean("show_delete_action", true));
        checkBoxShowOpenAppAction.setChecked(preferences.getBoolean("show_open_app_action", true));

        int audioFormatIndex = preferences.getInt("audio_format", 1); // Default to 16-bit PCM
        String[] audioFormats = getResources().getStringArray(R.array.audio_format_options);
        textViewCurrentAudioFormat.setText(audioFormats[audioFormatIndex]);

        String recordingsFolder = preferences.getString("recordings_folder", "/storage/emulated/0/Android/data/com.github.axet.audiorecorder/files");
        textViewCurrentRecordingsFolder.setText(recordingsFolder);

        int recordingSourceIndex = preferences.getInt("recording_source", 0); // Default to Mic
        String[] recordingSources = getResources().getStringArray(R.array.recording_source_options);
        textViewCurrentRecordingSource.setText(recordingSources[recordingSourceIndex]);

        int sampleRateIndex = preferences.getInt("sample_rate", 1); // Default to 16 kHz
        String[] sampleRates = getResources().getStringArray(R.array.sample_rate_options);
        textViewCurrentSampleRate.setText(sampleRates[sampleRateIndex]);

        int encodingIndex = preferences.getInt("encoding", 0); // Default to .ogg
        String[] encodings = getResources().getStringArray(R.array.encoding_options);
        textViewCurrentEncoding.setText(encodings[encodingIndex]);

        int audioChannelsIndex = preferences.getInt("audio_channels", 0); // Default to Mono
        String[] audioChannels = getResources().getStringArray(R.array.audio_channels_options);
        textViewCurrentAudioChannels.setText(audioChannels[audioChannelsIndex]);

        int filenameFormatIndex = preferences.getInt("filename_format", 0); // Default to first option
        String[] filenameFormats = getResources().getStringArray(R.array.filename_format_options);
        textViewCurrentFilenameFormat.setText(filenameFormats[filenameFormatIndex]);

        switchVoiceFilter.setChecked(preferences.getBoolean("voice_filter", false));
        int recordingVolume = preferences.getInt("recording_volume", 100);
        textViewCurrentRecordingVolume.setText(recordingVolume + "%");
        switchSkipSilence.setChecked(preferences.getBoolean("skip_silence", false));
        switchEncodingOnTheFly.setChecked(preferences.getBoolean("encoding_on_the_fly", false));
        switchPauseDuringCall.setChecked(preferences.getBoolean("pause_during_call", true));
        switchSilentMode.setChecked(preferences.getBoolean("silent_mode", false));
        switchLockscreenControls.setChecked(preferences.getBoolean("lockscreen_controls", false));

        int applicationThemeIndex = preferences.getInt("application_theme", 0); // Default to Theme White
        String[] applicationThemes = getResources().getStringArray(R.array.application_theme_options);
        textViewCurrentApplicationTheme.setText(applicationThemes[applicationThemeIndex]);
        applyApplicationTheme(applicationThemeIndex);
    }

    private void setupListeners() {
        switchNotificationsEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting("notifications_enabled", isChecked);
        });

        layoutNotificationSound.setOnClickListener(v -> showNotificationSoundDialog());
        layoutNotificationPriority.setOnClickListener(v -> showNotificationPriorityDialog());

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

        layoutAudioFormat.setOnClickListener(v -> showAudioFormatDialog());
        layoutRecordingsFolder.setOnClickListener(v -> openFolderPicker());
        layoutRecordingSource.setOnClickListener(v -> showRecordingSourceDialog());
        layoutSampleRate.setOnClickListener(v -> showSampleRateDialog());
        layoutEncoding.setOnClickListener(v -> showEncodingDialog());
        layoutAudioChannels.setOnClickListener(v -> showAudioChannelsDialog());
        layoutFilenameFormat.setOnClickListener(v -> showFilenameFormatDialog());
        switchVoiceFilter.setOnCheckedChangeListener((buttonView, isChecked) -> saveSetting("voice_filter", isChecked));
        layoutRecordingVolume.setOnClickListener(v -> showRecordingVolumeDialog());
        switchSkipSilence.setOnCheckedChangeListener((buttonView, isChecked) -> saveSetting("skip_silence", isChecked));
        switchEncodingOnTheFly.setOnCheckedChangeListener((buttonView, isChecked) -> saveSetting("encoding_on_the_fly", isChecked));
        switchPauseDuringCall.setOnCheckedChangeListener((buttonView, isChecked) -> saveSetting("pause_during_call", isChecked));
        switchSilentMode.setOnCheckedChangeListener((buttonView, isChecked) -> saveSetting("silent_mode", isChecked));
        switchLockscreenControls.setOnCheckedChangeListener((buttonView, isChecked) -> saveSetting("lockscreen_controls", isChecked));
        layoutApplicationTheme.setOnClickListener(v -> showApplicationThemeDialog());
    }

    private void saveSetting(String key, boolean value) {
        preferences.edit().putBoolean(key, value).apply();
        Toast.makeText(this, "Setting saved", Toast.LENGTH_SHORT).show();
    }

    private void saveSetting(String key, int value) {
        preferences.edit().putInt(key, value).apply();
        Toast.makeText(this, "Setting saved", Toast.LENGTH_SHORT).show();
    }

    private void saveSetting(String key, String value) {
        preferences.edit().putString(key, value).apply();
        Toast.makeText(this, "Setting saved", Toast.LENGTH_SHORT).show();
    }

    private void openFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        folderPickerLauncher.launch(intent);
    }

    private void showAudioFormatDialog() {
        String[] audioFormats = getResources().getStringArray(R.array.audio_format_options);
        int currentSelection = preferences.getInt("audio_format", 1);

        new AlertDialog.Builder(this)
                .setTitle("Audio Format")
                .setSingleChoiceItems(audioFormats, currentSelection, (dialog, which) -> {
                    saveSetting("audio_format", which);
                    textViewCurrentAudioFormat.setText(audioFormats[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRecordingSourceDialog() {
        String[] recordingSources = getResources().getStringArray(R.array.recording_source_options);
        int currentSelection = preferences.getInt("recording_source", 0);

        new AlertDialog.Builder(this)
                .setTitle("Recording Source")
                .setSingleChoiceItems(recordingSources, currentSelection, (dialog, which) -> {
                    if (which == 1) { // Bluetooth
                        showWarningDialog(getString(R.string.recording_source_warning_bluetooth), () -> {
                            saveSetting("recording_source", which);
                            textViewCurrentRecordingSource.setText(recordingSources[which]);
                        });
                    } else if (which == 2) { // Internal Audio
                        showWarningDialog(getString(R.string.recording_source_warning_internal), () -> {
                            saveSetting("recording_source", which);
                            textViewCurrentRecordingSource.setText(recordingSources[which]);
                        });
                    } else {
                        saveSetting("recording_source", which);
                        textViewCurrentRecordingSource.setText(recordingSources[which]);
                    }
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSampleRateDialog() {
        String[] sampleRates = getResources().getStringArray(R.array.sample_rate_options);
        int currentSelection = preferences.getInt("sample_rate", 1);

        new AlertDialog.Builder(this)
                .setTitle("Sample Rate")
                .setSingleChoiceItems(sampleRates, currentSelection, (dialog, which) -> {
                    saveSetting("sample_rate", which);
                    textViewCurrentSampleRate.setText(sampleRates[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEncodingDialog() {
        String[] encodings = getResources().getStringArray(R.array.encoding_options);
        int currentSelection = preferences.getInt("encoding", 0);

        new AlertDialog.Builder(this)
                .setTitle("Encoding")
                .setSingleChoiceItems(encodings, currentSelection, (dialog, which) -> {
                    saveSetting("encoding", which);
                    textViewCurrentEncoding.setText(encodings[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAudioChannelsDialog() {
        String[] audioChannels = getResources().getStringArray(R.array.audio_channels_options);
        int currentSelection = preferences.getInt("audio_channels", 0);

        new AlertDialog.Builder(this)
                .setTitle("Audio Channels")
                .setSingleChoiceItems(audioChannels, currentSelection, (dialog, which) -> {
                    saveSetting("audio_channels", which);
                    textViewCurrentAudioChannels.setText(audioChannels[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showFilenameFormatDialog() {
        String[] filenameFormats = getResources().getStringArray(R.array.filename_format_options);
        int currentSelection = preferences.getInt("filename_format", 0);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.filename_format_title))
                .setSingleChoiceItems(filenameFormats, currentSelection, (dialog, which) -> {
                    saveSetting("filename_format", which);
                    textViewCurrentFilenameFormat.setText(filenameFormats[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showNotificationSoundDialog() {
        String[] notificationSounds = getResources().getStringArray(R.array.notification_sound_options);
        int currentSelection = preferences.getInt("notification_sound", 0);

        new AlertDialog.Builder(this)
                .setTitle("Notification Sound")
                .setSingleChoiceItems(notificationSounds, currentSelection, (dialog, which) -> {
                    saveSetting("notification_sound", which);
                    textViewCurrentNotificationSound.setText(notificationSounds[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showNotificationPriorityDialog() {
        String[] notificationPriorities = getResources().getStringArray(R.array.notification_priority_options);
        int currentSelection = preferences.getInt("notification_priority", 1);

        new AlertDialog.Builder(this)
                .setTitle("Notification Priority")
                .setSingleChoiceItems(notificationPriorities, currentSelection, (dialog, which) -> {
                    saveSetting("notification_priority", which);
                    textViewCurrentNotificationPriority.setText(notificationPriorities[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRecordingVolumeDialog() {
        int currentVolume = preferences.getInt("recording_volume", 100);

        // Create custom view for dialog
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(32, 16, 32, 16);

        TextView volumeText = new TextView(this);
        volumeText.setText(currentVolume + "%");
        volumeText.setTextSize(18);
        volumeText.setGravity(android.view.Gravity.CENTER);
        volumeText.setPadding(0, 0, 0, 16);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(200);
        seekBar.setProgress(currentVolume);
        seekBar.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                volumeText.setText(progress + "%");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        dialogLayout.addView(volumeText);
        dialogLayout.addView(seekBar);

        new AlertDialog.Builder(this)
                .setTitle("Recording Volume")
                .setView(dialogLayout)
                .setPositiveButton("OK", (dialog, which) -> {
                    int newVolume = seekBar.getProgress();
                    saveSetting("recording_volume", newVolume);
                    textViewCurrentRecordingVolume.setText(newVolume + "%");
                })
                .setNeutralButton("Default", (dialog, which) -> {
                    seekBar.setProgress(100);
                    volumeText.setText("100%");
                    saveSetting("recording_volume", 100);
                    textViewCurrentRecordingVolume.setText("100%");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showApplicationThemeDialog() {
        String[] applicationThemes = getResources().getStringArray(R.array.application_theme_options);
        int currentSelection = preferences.getInt("application_theme", 0);

        new AlertDialog.Builder(this)
                .setTitle("Application Theme")
                .setSingleChoiceItems(applicationThemes, currentSelection, (dialog, which) -> {
                    saveSetting("application_theme", which);
                    textViewCurrentApplicationTheme.setText(applicationThemes[which]);
                    applyApplicationTheme(which);
                    recreate(); // Recreate activity to apply theme
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void applyApplicationTheme(int themeIndex) {
        int nightMode;
        switch (themeIndex) {
            case 0: // Theme White
                nightMode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case 1: // Theme Dark
                nightMode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            case 2: // System Default
            default:
                nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    private void showWarningDialog(String message, Runnable onConfirm) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.recording_source_warning_title))
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> onConfirm.run())
                .setNegativeButton("Cancel", null)
                .show();
    }
}