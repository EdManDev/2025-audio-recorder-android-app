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
import android.os.IBinder;
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
    private static final int NOTIFICATION_ID = 1001;

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

    private final IBinder binder = new RecordingBinder();

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
        createNotificationChannel();
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

            startForeground(NOTIFICATION_ID, createRecordingNotification());
            
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
            
            if (recordingListener != null) {
                recordingListener.onRecordingStopped(currentRecordingFile.getAbsolutePath(), duration);
            }
            
            Log.d(TAG, "Recording stopped: " + currentRecordingFile.getAbsolutePath());
            
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to stop recording", e);
            if (recordingListener != null) {
                recordingListener.onRecordingError("Failed to stop recording: " + e.getMessage());
            }
        }
        
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
                
                startForeground(NOTIFICATION_ID, createPausedNotification());
                
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
                
                startForeground(NOTIFICATION_ID, createRecordingNotification());
                
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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Audio Recording",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Notifications for audio recording");
            channel.setSound(null, null);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createRecordingNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, RecordingService.class);
        stopIntent.setAction(ACTION_STOP_RECORDING);
        PendingIntent stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Recording Audio")
            .setContentText("Tap to open app")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build();
    }

    private Notification createPausedNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Recording Paused")
            .setContentText("Tap to open app")
            .setSmallIcon(android.R.drawable.ic_media_pause)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build();
    }

    @Override
    public void onDestroy() {
        if (isRecording) {
            stopRecording();
        }
        super.onDestroy();
    }
}