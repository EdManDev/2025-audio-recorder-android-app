package com.example.a2025audiorecorderandroidapp.utils;

import android.content.Context;
import android.os.Environment;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileUtils {
    
    public static final String RECORDINGS_FOLDER = "AudioRecordings";
    public static final String AUDIO_FILE_EXTENSION = ".m4a";

    public static File getRecordingsDirectory(Context context) {
        File recordingsDir;
        
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            // Use app-specific external storage directory (no permissions needed)
            recordingsDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), RECORDINGS_FOLDER);
        } else {
            // Fall back to internal storage
            recordingsDir = new File(context.getFilesDir(), RECORDINGS_FOLDER);
        }
        
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs();
        }
        
        return recordingsDir;
    }

    public static File createNewRecordingFile(Context context) {
        File recordingsDir = getRecordingsDirectory(context);
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        String fileName = "Recording_" + timestamp + AUDIO_FILE_EXTENSION;
        
        return new File(recordingsDir, fileName);
    }

    public static String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        seconds = seconds % 60;
        minutes = minutes % 60;
        
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }

    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format(Locale.getDefault(), "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    public static boolean deleteRecording(String filePath) {
        File file = new File(filePath);
        return file.exists() && file.delete();
    }

    public static boolean renameRecording(String filePath, String newName) {
        File oldFile = new File(filePath);
        if (!oldFile.exists()) {
            return false;
        }
        
        // Ensure the new name has the correct extension
        if (!newName.endsWith(AUDIO_FILE_EXTENSION)) {
            newName += AUDIO_FILE_EXTENSION;
        }
        
        File newFile = new File(oldFile.getParent(), newName);
        
        // Check if a file with the new name already exists
        if (newFile.exists()) {
            return false;
        }
        
        return oldFile.renameTo(newFile);
    }
}