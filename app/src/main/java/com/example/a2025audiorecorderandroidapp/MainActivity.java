package com.example.a2025audiorecorderandroidapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.StatFs;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;

import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a2025audiorecorderandroidapp.adapter.RecordingsAdapter;
import com.example.a2025audiorecorderandroidapp.model.Recording;
import com.example.a2025audiorecorderandroidapp.service.RecordingService;
import com.example.a2025audiorecorderandroidapp.utils.FileUtils;
import com.example.a2025audiorecorderandroidapp.utils.PermissionUtils;
import com.example.a2025audiorecorderandroidapp.utils.WaveformView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements RecordingService.RecordingListener {

    private FloatingActionButton btnRecord, btnPause, btnStop;
    private TextView recordingStatus, recordingTimer, emptyMessage;
    private RecyclerView recordingsList;
    private RecordingsAdapter adapter;
    private WaveformView waveformView;
    private List<Recording> allRecordings = new ArrayList<>();

    private RecordingService recordingService;
    private boolean serviceBound = false;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    private MediaPlayer mediaPlayer;
    private Recording currentPlayingRecording;
    private Runnable playbackUpdateRunnable;

    private final ActivityResultLauncher<String[]> requestPermissionsLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            boolean allGranted = true;
            for (Boolean granted : result.values()) {
                if (!granted) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                startRecording();
            } else {
                Toast.makeText(this, "Permissions required for recording", Toast.LENGTH_SHORT).show();
            }
        });

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RecordingService.RecordingBinder binder = (RecordingService.RecordingBinder) service;
            recordingService = binder.getService();
            recordingService.setRecordingListener(MainActivity.this);
            serviceBound = true;
            updateUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            recordingService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ensure action bar is shown
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Audio Recorder");
        }

        initViews();
        setupRecyclerView();
        setupClickListeners();
        loadRecordings();
        updateEmptyState();
        handleIntent(getIntent());
    }

    private void initViews() {
        btnRecord = findViewById(R.id.btnRecord);
        btnPause = findViewById(R.id.btnPause);
        btnStop = findViewById(R.id.btnStop);
        recordingStatus = findViewById(R.id.recordingStatus);
        recordingTimer = findViewById(R.id.recordingTimer);
        recordingsList = findViewById(R.id.recordingsList);
        emptyMessage = findViewById(R.id.emptyMessage);
        waveformView = findViewById(R.id.waveformView);
    }

    private void setupRecyclerView() {
        adapter = new RecordingsAdapter(new RecordingsAdapter.OnRecordingClickListener() {
            @Override
            public void onPlayClick(Recording recording) {
                handlePlayPauseClick(recording);
            }

            @Override
            public void onLongPress(Recording recording) {
                showContextMenu(recording);
            }

            @Override
            public void onSeek(Recording recording, int position) {
                seekToPosition(recording, position);
            }
        });

        recordingsList.setLayoutManager(new LinearLayoutManager(this));
        recordingsList.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnRecord.setOnClickListener(v -> {
            if (PermissionUtils.hasRecordingPermissions(this)) {
                startRecording();
            } else {
                requestPermissionsLauncher.launch(PermissionUtils.RECORDING_PERMISSIONS);
            }
        });

        btnPause.setOnClickListener(v -> {
            if (serviceBound && recordingService != null) {
                if (recordingService.isPaused()) {
                    recordingService.resumeRecording();
                } else {
                    recordingService.pauseRecording();
                }
            }
        });

        btnStop.setOnClickListener(v -> {
            if (serviceBound && recordingService != null) {
                recordingService.stopRecording();
            }
        });
    }

    private void startRecording() {
        // Check available storage
        if (!hasEnoughStorage()) {
            Toast.makeText(this, "Not enough storage space. Please free up space before recording.", Toast.LENGTH_LONG).show();
            return;
        }

        Intent serviceIntent = new Intent(this, RecordingService.class);
        serviceIntent.setAction(RecordingService.ACTION_START_RECORDING);
        startForegroundService(serviceIntent);

        if (!serviceBound) {
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private boolean hasEnoughStorage() {
        try {
            File recordingsDir = FileUtils.getRecordingsDirectory(this);
            StatFs stat = new StatFs(recordingsDir.getPath());
            long availableBytes = stat.getAvailableBytes();
            // Require at least 10MB free space
            return availableBytes > 10 * 1024 * 1024;
        } catch (Exception e) {
            // If we can't check, assume it's OK
            return true;
        }
    }

    private void updateUI() {
        if (serviceBound && recordingService != null) {
            boolean isRecording = recordingService.isRecording();
            boolean isPaused = recordingService.isPaused();

            btnRecord.setVisibility(isRecording ? View.GONE : View.VISIBLE);
            btnPause.setVisibility(isRecording ? View.VISIBLE : View.GONE);
            btnStop.setVisibility(isRecording ? View.VISIBLE : View.GONE);

            if (isRecording) {
                waveformView.setVisibility(View.VISIBLE);
                if (isPaused) {
                    recordingStatus.setText("Recording Paused");
                    btnPause.setImageResource(android.R.drawable.ic_media_play);
                    stopTimer();
                } else {
                    recordingStatus.setText("Recording...");
                    btnPause.setImageResource(android.R.drawable.ic_media_pause);
                    startTimer();
                }
            } else {
                recordingStatus.setText("Ready to Record");
                recordingTimer.setText("00:00");
                waveformView.setVisibility(View.GONE);
                waveformView.clear();
                stopTimer();
            }
        } else {
            btnRecord.setVisibility(View.VISIBLE);
            btnPause.setVisibility(View.GONE);
            btnStop.setVisibility(View.GONE);
            recordingStatus.setText("Ready to Record");
            recordingTimer.setText("00:00");
            waveformView.setVisibility(View.GONE);
            waveformView.clear();
            stopTimer();
        }
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (serviceBound && recordingService != null) {
                    long duration = recordingService.getRecordingDuration();
                    recordingTimer.setText(FileUtils.formatDuration(duration));
                    
                    // Animate waveform during recording
                    if (recordingService.isRecording() && !recordingService.isPaused()) {
                        waveformView.simulateAmplitude();
                    }
                    
                    timerHandler.postDelayed(this, 200);
                }
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void stopTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
    }

    private void loadRecordings() {
        File recordingsDir = FileUtils.getRecordingsDirectory(this);
        File[] files = recordingsDir.listFiles((dir, name) ->
            name.toLowerCase().endsWith(FileUtils.AUDIO_FILE_EXTENSION));

        allRecordings.clear();
        if (files != null) {
            Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
            for (File file : files) {
                Recording recording = new Recording(file);
                // Load duration for progress bar display
                try {
                    MediaPlayer mp = new MediaPlayer();
                    mp.setDataSource(file.getAbsolutePath());
                    mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mp.prepare();
                    recording.setDuration(mp.getDuration());
                    mp.release();
                } catch (IOException e) {
                    // If we can't get duration, use 0
                    recording.setDuration(0);
                }
                allRecordings.add(recording);
            }
        }

        filterRecordings("");
        updateEmptyState();
    }



    private void filterRecordings(String query) {
        List<Recording> filteredRecordings = new ArrayList<>();
        for (Recording recording : allRecordings) {
            if (recording.getFileName().toLowerCase().contains(query.toLowerCase())) {
                filteredRecordings.add(recording);
            }
        }
        adapter.setRecordings(filteredRecordings);
    }

    private void updateEmptyState() {
        boolean isEmpty = adapter.getItemCount() == 0;
        emptyMessage.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recordingsList.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void handlePlayPauseClick(Recording recording) {
        // If this recording is currently playing, pause it
        if (currentPlayingRecording != null && 
            currentPlayingRecording.getFilePath().equals(recording.getFilePath()) && 
            mediaPlayer != null && mediaPlayer.isPlaying()) {
            
            mediaPlayer.pause();
            // Update current position before pausing
            currentPlayingRecording.setCurrentPosition(mediaPlayer.getCurrentPosition());
            currentPlayingRecording.setIsPlaying(false);
            stopPlaybackUpdates();
            adapter.notifyDataSetChanged();
            return;
        }
        
        // If this recording is paused, resume it
        if (currentPlayingRecording != null && 
            currentPlayingRecording.getFilePath().equals(recording.getFilePath()) && 
            mediaPlayer != null && !mediaPlayer.isPlaying()) {
            
            mediaPlayer.start();
            currentPlayingRecording.setIsPlaying(true);
            startPlaybackUpdates();
            adapter.notifyDataSetChanged();
            return;
        }
        
        // Start playing this recording (new recording)
        startPlayingRecording(recording);
    }

    private void startPlayingRecording(Recording recording) {
        try {
            // Reset position of any previously playing recording
            if (currentPlayingRecording != null && 
                !currentPlayingRecording.getFilePath().equals(recording.getFilePath())) {
                currentPlayingRecording.setIsPlaying(false);
                currentPlayingRecording.setCurrentPosition(0);
            }
            
            // Stop any existing playback
            if (mediaPlayer != null) {
                mediaPlayer.release();
                stopPlaybackUpdates();
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(recording.getFilePath());
            
            // Set audio attributes for optimal playback volume
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setVolume(1.0f, 1.0f); // Set volume to maximum
            
            mediaPlayer.prepare();
            
            // Set the total duration in the recording object if not already set
            if (recording.getDuration() == 0) {
                recording.setDuration(mediaPlayer.getDuration());
            }
            
            // If this recording has a saved position, seek to it
            if (recording.getCurrentPosition() > 0) {
                mediaPlayer.seekTo(recording.getCurrentPosition());
            }
            
            mediaPlayer.start();
            
            currentPlayingRecording = recording;
            currentPlayingRecording.setIsPlaying(true);
            startPlaybackUpdates();

            Toast.makeText(this, "Playing: " + recording.getFileName(), Toast.LENGTH_SHORT).show();

            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                mediaPlayer = null;
                if (currentPlayingRecording != null) {
                    currentPlayingRecording.setIsPlaying(false);
                    currentPlayingRecording.setCurrentPosition(0);
                }
                currentPlayingRecording = null;
                stopPlaybackUpdates();
                adapter.notifyDataSetChanged();
            });

            adapter.notifyDataSetChanged();

        } catch (IOException e) {
            Toast.makeText(this, "Error playing recording", Toast.LENGTH_SHORT).show();
        }
    }

    private void seekToPosition(Recording recording, int position) {
        // Only allow seeking if this recording is currently loaded in the MediaPlayer
        if (currentPlayingRecording != null && 
            currentPlayingRecording.getFilePath().equals(recording.getFilePath()) && 
            mediaPlayer != null) {
            
            try {
                mediaPlayer.seekTo(position);
                recording.setCurrentPosition(position);
                adapter.notifyDataSetChanged();
            } catch (IllegalStateException e) {
                // MediaPlayer might not be in a valid state for seeking
                Toast.makeText(this, "Cannot seek at this time", Toast.LENGTH_SHORT).show();
            }
        } else {
            // If the recording is not currently playing, start playing from the seek position
            startPlayingRecordingAtPosition(recording, position);
        }
    }

    private void startPlayingRecordingAtPosition(Recording recording, int position) {
        try {
            // Stop any existing playback
            if (mediaPlayer != null) {
                mediaPlayer.release();
                stopPlaybackUpdates();
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(recording.getFilePath());
            
            // Set audio attributes for optimal playback volume
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setVolume(1.0f, 1.0f); // Set volume to maximum
            
            mediaPlayer.prepare();
            
            // Set the total duration in the recording object if not already set
            if (recording.getDuration() == 0) {
                recording.setDuration(mediaPlayer.getDuration());
            }
            
            // Seek to the desired position before starting
            mediaPlayer.seekTo(position);
            mediaPlayer.start();
            
            currentPlayingRecording = recording;
            currentPlayingRecording.setIsPlaying(true);
            currentPlayingRecording.setCurrentPosition(position);
            startPlaybackUpdates();

            Toast.makeText(this, "Playing: " + recording.getFileName(), Toast.LENGTH_SHORT).show();

            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                mediaPlayer = null;
                if (currentPlayingRecording != null) {
                    currentPlayingRecording.setIsPlaying(false);
                    currentPlayingRecording.setCurrentPosition(0);
                }
                currentPlayingRecording = null;
                stopPlaybackUpdates();
                adapter.notifyDataSetChanged();
            });

            adapter.notifyDataSetChanged();

        } catch (IOException e) {
            Toast.makeText(this, "Error playing recording", Toast.LENGTH_SHORT).show();
        }
    }

    private void startPlaybackUpdates() {
        playbackUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying() && currentPlayingRecording != null) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    int duration = mediaPlayer.getDuration();
                    
                    // Update the recording object with current playback position
                    currentPlayingRecording.setCurrentPosition(currentPosition);
                    currentPlayingRecording.setIsPlaying(true);
                    
                    adapter.notifyDataSetChanged();
                    timerHandler.postDelayed(this, 500);
                }
            }
        };
        timerHandler.post(playbackUpdateRunnable);
    }

    private void stopPlaybackUpdates() {
        if (playbackUpdateRunnable != null) {
            timerHandler.removeCallbacks(playbackUpdateRunnable);
            playbackUpdateRunnable = null;
        }
        // Don't reset position here - only set playing state to false
        // Position should only be reset when playback completes or a new recording starts
    }

    private void shareRecording(Recording recording) {
        try {
            File file = new File(recording.getFilePath());
            Uri uri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider", file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("audio/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share Recording"));
        } catch (Exception e) {
            Toast.makeText(this, "Error sharing recording", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmation(Recording recording) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Recording")
            .setMessage("Are you sure you want to delete \"" + recording.getFileName() + "\"?")
            .setPositiveButton("Delete", (dialog, which) -> deleteRecording(recording))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteRecording(Recording recording) {
        if (FileUtils.deleteRecording(recording.getFilePath())) {
            loadRecordings();
            Toast.makeText(this, "Recording deleted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Error deleting recording", Toast.LENGTH_SHORT).show();
        }
    }

    private void showContextMenu(Recording recording) {
        String[] options = {"Share", "Rename", "Delete"};
        
        new AlertDialog.Builder(this)
            .setTitle(recording.getFileName())
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // Share
                        shareRecording(recording);
                        break;
                    case 1: // Rename
                        showRenameDialog(recording);
                        break;
                    case 2: // Delete
                        showDeleteConfirmation(recording);
                        break;
                }
            })
            .show();
    }

    private void showRenameDialog(Recording recording) {
        EditText editText = new EditText(this);
        String currentName = recording.getFileName();
        String nameWithoutExtension = currentName.substring(0, currentName.lastIndexOf('.'));
        editText.setText(nameWithoutExtension);
        editText.setSelection(nameWithoutExtension.length());

        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.rename_recording))
            .setMessage(getString(R.string.enter_new_name))
            .setView(editText)
            .setPositiveButton(getString(R.string.rename), (dialog, which) -> {
                String newName = editText.getText().toString().trim();
                if (!newName.isEmpty()) {
                    renameRecording(recording, newName);
                }
            })
            .setNegativeButton(getString(R.string.cancel), null)
            .show();
    }

    private void renameRecording(Recording recording, String newName) {
        if (FileUtils.renameRecording(recording.getFilePath(), newName)) {
            loadRecordings();
            Toast.makeText(this, getString(R.string.recording_renamed), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getString(R.string.error_renaming), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent serviceIntent = new Intent(this, RecordingService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            String filePath = intent.getStringExtra("file_path");

            if (filePath != null) {
                Recording recording = findRecordingByPath(filePath);
                if (recording != null) {
                    switch (action) {
                        case "PLAY_RECORDING":
                            handlePlayPauseClick(recording);
                            break;
                        case "SHARE_RECORDING":
                            shareRecording(recording);
                            break;
                        case "DELETE_RECORDING":
                            showDeleteConfirmation(recording);
                            break;
                    }
                }
            }
        }
    }

    private Recording findRecordingByPath(String filePath) {
        for (Recording recording : adapter.getRecordings()) {
            if (recording.getFilePath().equals(filePath)) {
                return recording;
            }
        }
        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        // Configure the search menu item
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint("Search recordings...");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterRecordings(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterRecordings(newText);
                return true;
            }
        });

        // Handle search close
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                filterRecordings("");
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        stopPlaybackUpdates();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    // RecordingService.RecordingListener implementation
    @Override
    public void onRecordingStarted(String filePath) {
        runOnUiThread(this::updateUI);
    }

    @Override
    public void onRecordingStopped(String filePath, long duration) {
        runOnUiThread(() -> {
            updateUI();
            // Add small delay to ensure file is completely written
            new Handler().postDelayed(() -> {
                loadRecordings();
                Toast.makeText(this, "Recording saved", Toast.LENGTH_SHORT).show();
            }, 100);
        });
    }

    @Override
    public void onRecordingPaused() {
        runOnUiThread(this::updateUI);
    }

    @Override
    public void onRecordingResumed() {
        runOnUiThread(this::updateUI);
    }

    @Override
    public void onRecordingError(String error) {
        runOnUiThread(() -> {
            updateUI();
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        });
    }
}