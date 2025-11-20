package com.example.a2025audiorecorderandroidapp.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.a2025audiorecorderandroidapp.MainActivity;
import com.example.a2025audiorecorderandroidapp.R;
import com.example.a2025audiorecorderandroidapp.utils.FileUtils;

import java.io.File;
import java.io.IOException;

public class RecordingService extends Service {
    private static final String TAG = "RecordingService";
    private static final String CHANNEL_ID = "AudioRecordingChannel";
    private static final String POST_RECORDING_CHANNEL_ID = "PostRecordingChannel";
    private static final String BATTERY_WARNING_CHANNEL_ID = "BatteryWarningChannel";
    private static final int NOTIFICATION_ID = 1001;
    private static final int POST_RECORDING_NOTIFICATION_ID = 1002;
    private static final int BATTERY_WARNING_NOTIFICATION_ID = 1003;
    private static final long BATTERY_WARNING_DURATION_MS = 30 * 60 * 1000; // 30 minutes
    private static final long TIME_LIMIT_WARNING_MS = 55 * 60 * 1000; // 55 minutes
    private static final long MAX_RECORDING_DURATION_MS = 60 * 60 * 1000; // 60 minutes

    public static final String ACTION_START_RECORDING = "START_RECORDING";
    public static final String ACTION_STOP_RECORDING = "STOP_RECORDING";
    public static final String ACTION_PAUSE_RECORDING = "PAUSE_RECORDING";
    public static final String ACTION_RESUME_RECORDING = "RESUME_RECORDING";

    private MediaRecorder mediaRecorder;
    private File currentRecordingFile;
    private boolean isRecording = false;
    private boolean isPaused = false;
    private long recordingStartTime = 0;
    private long pausedTime = 0;
    private boolean batteryWarningShown = false;
    private boolean timeLimitWarningShown = false;
    private boolean isCancelling = false;

    private final IBinder binder = new RecordingBinder();
    private Handler notificationHandler;
    private Runnable notificationUpdater;

    public interface RecordingListener {
        void onRecordingStarted(String filePath);
        void onRecordingStopped(String filePath, long duration);
        void onRecordingPaused();
        void onRecordingResumed();
        void onRecordingError(String error);
    }

    private RecordingListener recordingListener;

    public class RecordingBinder extends Binder {
        public RecordingService getService() {
            return RecordingService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationHandler = new Handler(Looper.getMainLooper());
        createNotificationChannels();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_START_RECORDING:
                        startRecording();
                        break;
                    case ACTION_STOP_RECORDING:
                        stopRecording();
                        break;
                    case ACTION_PAUSE_RECORDING:
                        pauseRecording();
                        break;
                    case ACTION_RESUME_RECORDING:
                        resumeRecording();
                        break;
                }
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setRecordingListener(RecordingListener listener) {
        this.recordingListener = listener;
    }

    public void setCancelling(boolean cancelling) {
        this.isCancelling = cancelling;
    }

    public void startRecording() {
        if (isRecording) {
            Log.w(TAG, "Recording already in progress");
            return;
        }

        try {
            currentRecordingFile = FileUtils.createNewRecordingFile(this);
            
            // Configure audio manager for optimal recording
            AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (audioManager != null) {
                // Set microphone gain to maximum for better volume
                audioManager.setMode(AudioManager.MODE_NORMAL);
            }
            
            mediaRecorder = new MediaRecorder();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            } else {
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            }
            
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            
            // Enhanced audio settings for better volume and quality
            mediaRecorder.setAudioSamplingRate(48000); // Higher sampling rate
            mediaRecorder.setAudioEncodingBitRate(256000); // Higher bitrate for better quality
            mediaRecorder.setAudioChannels(2); // Stereo recording
            
            mediaRecorder.setOutputFile(currentRecordingFile.getAbsolutePath());

            mediaRecorder.prepare();
            mediaRecorder.start();
            
            isRecording = true;
            isPaused = false;
            recordingStartTime = System.currentTimeMillis();
            pausedTime = 0;
            batteryWarningShown = false;
            timeLimitWarningShown = false;

            startForeground(NOTIFICATION_ID, createRecordingNotification());
            startNotificationUpdater();

            if (recordingListener != null) {
                recordingListener.onRecordingStarted(currentRecordingFile.getAbsolutePath());
            }
            
            Log.d(TAG, "Recording started: " + currentRecordingFile.getAbsolutePath());
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to start recording", e);
            if (recordingListener != null) {
                recordingListener.onRecordingError("Failed to start recording: " + e.getMessage());
            }
            stopSelf();
        }
    }

    public void stopRecording() {
        if (!isRecording) {
            Log.w(TAG, "No recording in progress");
            return;
        }

        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            
            long duration = System.currentTimeMillis() - recordingStartTime - pausedTime;
            
            isRecording = false;
            isPaused = false;
            batteryWarningShown = false;
            timeLimitWarningShown = false;
            
            if (recordingListener != null) {
                recordingListener.onRecordingStopped(currentRecordingFile.getAbsolutePath(), duration);
            }
            
            Log.d(TAG, "Recording stopped: " + currentRecordingFile.getAbsolutePath());

            // Show post-recording notification only if not cancelling
            if (!isCancelling) {
                showPostRecordingNotification(currentRecordingFile, duration);
            }

            // Reset the cancelling flag
            isCancelling = false;

        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to stop recording", e);
            if (recordingListener != null) {
                recordingListener.onRecordingError("Failed to stop recording: " + e.getMessage());
            }
        }

        stopNotificationUpdater();
        stopForeground(true);
        stopSelf();
    }

    public void pauseRecording() {
        if (!isRecording || isPaused) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder.pause();
                isPaused = true;
                pausedTime = System.currentTimeMillis();
                
                if (recordingListener != null) {
                    recordingListener.onRecordingPaused();
                }

                updateNotification();
                
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to pause recording", e);
            }
        }
    }

    public void resumeRecording() {
        if (!isRecording || !isPaused) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder.resume();
                pausedTime = System.currentTimeMillis() - pausedTime;
                isPaused = false;
                
                if (recordingListener != null) {
                    recordingListener.onRecordingResumed();
                }

                updateNotification();
                
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to resume recording", e);
            }
        }
    }

    public boolean isRecording() {
        return isRecording;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public long getRecordingDuration() {
        if (!isRecording) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        long duration = currentTime - recordingStartTime;
        
        if (isPaused) {
            duration -= (currentTime - pausedTime);
        } else {
            duration -= pausedTime;
        }
        
        return Math.max(0, duration);
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Recording channel
            NotificationChannel recordingChannel = new NotificationChannel(
                CHANNEL_ID,
                "Audio Recording",
                NotificationManager.IMPORTANCE_LOW
            );
            recordingChannel.setDescription("Notifications for active audio recording");
            recordingChannel.setSound(null, null);

            // Post-recording channel
            NotificationChannel postRecordingChannel = new NotificationChannel(
                POST_RECORDING_CHANNEL_ID,
                "Recording Saved",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            postRecordingChannel.setDescription("Notifications when recordings are saved");

            // Battery warning channel
            NotificationChannel batteryWarningChannel = new NotificationChannel(
                BATTERY_WARNING_CHANNEL_ID,
                "Battery Warning",
                NotificationManager.IMPORTANCE_HIGH
            );
            batteryWarningChannel.setDescription("Warnings about battery usage during long recordings");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(recordingChannel);
            notificationManager.createNotificationChannel(postRecordingChannel);
            notificationManager.createNotificationChannel(batteryWarningChannel);
        }
    }

    private void startNotificationUpdater() {
        notificationUpdater = new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    updateNotification();
                    checkBatteryWarning();
                    checkTimeLimit();
                    notificationHandler.postDelayed(this, 1000); // Update every second
                }
            }
        };
        notificationHandler.post(notificationUpdater);
    }

    private void stopNotificationUpdater() {
        if (notificationUpdater != null) {
            notificationHandler.removeCallbacks(notificationUpdater);
            notificationUpdater = null;
        }
    }

    private void updateNotification() {
        Notification notification = isPaused ? createPausedNotification() : createRecordingNotification();
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private void checkBatteryWarning() {
        if (!batteryWarningShown && getRecordingDuration() > BATTERY_WARNING_DURATION_MS) {
            showBatteryWarningNotification();
            batteryWarningShown = true;
        }
    }

    private void checkTimeLimit() {
        long duration = getRecordingDuration();
        if (!timeLimitWarningShown && duration > TIME_LIMIT_WARNING_MS) {
            showTimeLimitWarningNotification();
            timeLimitWarningShown = true;
        } else if (duration > MAX_RECORDING_DURATION_MS) {
            // Auto-stop recording
            stopRecording();
        }
    }

    private void showBatteryWarningNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 9, intent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, BATTERY_WARNING_CHANNEL_ID)
            .setContentTitle("Battery Usage Warning")
            .setContentText("Recording has been active for over 30 minutes. Consider stopping to save battery.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build();

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.notify(BATTERY_WARNING_NOTIFICATION_ID, notification);
        }
    }

    private void showTimeLimitWarningNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 10, intent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, BATTERY_WARNING_CHANNEL_ID)
            .setContentTitle("Recording Time Limit Warning")
            .setContentText("Recording will automatically stop in 5 minutes. Consider stopping now to save the recording.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build();

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.notify(BATTERY_WARNING_NOTIFICATION_ID + 1, notification);
        }
    }

    private Notification createRecordingNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Intent pauseIntent = new Intent(this, RecordingService.class);
        pauseIntent.setAction(ACTION_PAUSE_RECORDING);
        PendingIntent pausePendingIntent = PendingIntent.getService(
            this, 1, pauseIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, RecordingService.class);
        stopIntent.setAction(ACTION_STOP_RECORDING);
        PendingIntent stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        long duration = getRecordingDuration();
        String durationText = formatDuration(duration);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Recording Audio")
            .setContentText(durationText + " - Tap to open app")
            .setSubText(currentRecordingFile != null ? currentRecordingFile.getName() : "")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
            .addAction(android.R.drawable.ic_media_ff, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    private Notification createPausedNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Intent resumeIntent = new Intent(this, RecordingService.class);
        resumeIntent.setAction(ACTION_RESUME_RECORDING);
        PendingIntent resumePendingIntent = PendingIntent.getService(
            this, 3, resumeIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, RecordingService.class);
        stopIntent.setAction(ACTION_STOP_RECORDING);
        PendingIntent stopPendingIntent = PendingIntent.getService(
            this, 4, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        long duration = getRecordingDuration();
        String durationText = formatDuration(duration);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Recording Paused")
            .setContentText(durationText + " - Tap to open app")
            .setSubText(currentRecordingFile != null ? currentRecordingFile.getName() : "")
            .setSmallIcon(android.R.drawable.ic_media_pause)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_play, "Resume", resumePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    private void showPostRecordingNotification(File recordingFile, long duration) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 5, intent, PendingIntent.FLAG_IMMUTABLE);

        // Play action
        Intent playIntent = new Intent(this, MainActivity.class);
        playIntent.setAction("PLAY_RECORDING");
        playIntent.putExtra("file_path", recordingFile.getAbsolutePath());
        PendingIntent playPendingIntent = PendingIntent.getActivity(
            this, 6, playIntent, PendingIntent.FLAG_IMMUTABLE);

        // Share action
        Intent shareIntent = new Intent(this, MainActivity.class);
        shareIntent.setAction("SHARE_RECORDING");
        shareIntent.putExtra("file_path", recordingFile.getAbsolutePath());
        PendingIntent sharePendingIntent = PendingIntent.getActivity(
            this, 7, shareIntent, PendingIntent.FLAG_IMMUTABLE);

        // Delete action
        Intent deleteIntent = new Intent(this, MainActivity.class);
        deleteIntent.setAction("DELETE_RECORDING");
        deleteIntent.putExtra("file_path", recordingFile.getAbsolutePath());
        PendingIntent deletePendingIntent = PendingIntent.getActivity(
            this, 8, deleteIntent, PendingIntent.FLAG_IMMUTABLE);

        String durationText = FileUtils.formatDuration(duration);
        String sizeText = FileUtils.formatFileSize(recordingFile.length());

        Notification notification = new NotificationCompat.Builder(this, POST_RECORDING_CHANNEL_ID)
            .setContentTitle("Recording Saved")
            .setContentText(recordingFile.getName())
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText("Recording saved successfully\nDuration: " + durationText + "\nSize: " + sizeText + "\nLocation: " + recordingFile.getParent()))
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_play, "Play", playPendingIntent)
            .addAction(android.R.drawable.ic_menu_share, "Share", sharePendingIntent)
            .addAction(android.R.drawable.ic_menu_delete, "Delete", deletePendingIntent)
            .setAutoCancel(true)
            .build();

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.notify(POST_RECORDING_NOTIFICATION_ID, notification);
        }
    }

    private String formatDuration(long durationMs) {
        return FileUtils.formatDuration(durationMs);
    }

    @Override
    public void onDestroy() {
        stopNotificationUpdater();
        if (isRecording) {
            stopRecording();
        }
        super.onDestroy();
    }
}