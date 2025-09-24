package com.example.a2025audiorecorderandroidapp.model;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Recording {
    private final String fileName;
    private final String filePath;
    private long duration;
    private final long fileSize;
    private final long timestamp;
    private boolean isPlaying = false;
    private int currentPosition = 0;

    public Recording(File file) {
        this.fileName = file.getName();
        this.filePath = file.getAbsolutePath();
        this.fileSize = file.length();
        this.timestamp = file.lastModified();
        this.duration = 0; // Will be calculated when needed
    }

    // Getters
    public String getFileName() { return fileName; }
    public String getFilePath() { return filePath; }
    public long getDuration() { return duration; }
    public long getFileSize() { return fileSize; }
    public long getTimestamp() { return timestamp; }

    // Setters
    public void setDuration(long duration) { this.duration = duration; }
    public void setIsPlaying(boolean isPlaying) { this.isPlaying = isPlaying; }
    public void setCurrentPosition(int currentPosition) { this.currentPosition = currentPosition; }
    
    // Playback getters
    public boolean isPlaying() { return isPlaying; }
    public int getCurrentPosition() { return currentPosition; }

    // Utility methods
    public String getFormattedDuration() {
        if (isPlaying && currentPosition > 0) {
            // Show current position during playback
            long seconds = currentPosition / 1000;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        } else {
            // Show total duration when not playing
            long seconds = duration / 1000;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", fileSize / 1024.0);
        } else {
            return String.format(Locale.getDefault(), "%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }

}