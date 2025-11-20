package com.example.a2025audiorecorderandroidapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a2025audiorecorderandroidapp.R;
import com.example.a2025audiorecorderandroidapp.model.Recording;

import java.util.ArrayList;
import java.util.List;

public class RecordingsAdapter extends RecyclerView.Adapter<RecordingsAdapter.RecordingViewHolder> {

    public interface OnRecordingClickListener {
        void onPlayClick(Recording recording);
        void onLongPress(Recording recording);
        void onSeek(Recording recording, int position);
    }

    private final List<Recording> recordings = new ArrayList<>();
    private final OnRecordingClickListener listener;

    public RecordingsAdapter(OnRecordingClickListener listener) {
        this.listener = listener;
    }

    public void setRecordings(List<Recording> newRecordings) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new RecordingDiffCallback(this.recordings, newRecordings));
        this.recordings.clear();
        this.recordings.addAll(newRecordings);
        diffResult.dispatchUpdatesTo(this);
    }

    private static class RecordingDiffCallback extends DiffUtil.Callback {
        private final List<Recording> oldList;
        private final List<Recording> newList;

        public RecordingDiffCallback(List<Recording> oldList, List<Recording> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getFilePath().equals(newList.get(newItemPosition).getFilePath());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Recording oldRecording = oldList.get(oldItemPosition);
            Recording newRecording = newList.get(newItemPosition);
            
            return oldRecording.getFileName().equals(newRecording.getFileName()) &&
                   oldRecording.getDuration() == newRecording.getDuration() &&
                   oldRecording.getCurrentPosition() == newRecording.getCurrentPosition() &&
                   oldRecording.isPlaying() == newRecording.isPlaying() &&
                   oldRecording.getFileSize() == newRecording.getFileSize() &&
                   oldRecording.getTimestamp() == newRecording.getTimestamp();
        }
    }

    @NonNull
    @Override
    public RecordingsAdapter.RecordingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_recording, parent, false);
        return new RecordingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordingsAdapter.RecordingViewHolder holder, int position) {
        Recording recording = recordings.get(position);
        holder.bind(recording, listener);
    }

    @Override
    public int getItemCount() {
        return recordings.size();
    }

    public List<Recording> getRecordings() {
        return new ArrayList<>(recordings);
    }

    public static class RecordingViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvFileName;
        private final TextView tvDuration;
        private final TextView tvFileInfo;
        private final ImageView ivPlay;
        private final SeekBar seekBar;

        public RecordingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvFileInfo = itemView.findViewById(R.id.tvFileInfo);
            ivPlay = itemView.findViewById(R.id.ivPlay);
            seekBar = itemView.findViewById(R.id.seekBar);
        }

        public void bind(Recording recording, OnRecordingClickListener listener) {
            tvFileName.setText(recording.getFileName());
            tvDuration.setText(recording.getFormattedDuration());
            tvFileInfo.setText(itemView.getContext().getString(R.string.file_info_format, 
                recording.getFormattedFileSize(), recording.getFormattedDate()));

            // Update play/pause icon based on playback state
            if (recording.isPlaying()) {
                ivPlay.setImageResource(android.R.drawable.ic_media_pause);
            } else {
                ivPlay.setImageResource(android.R.drawable.ic_media_play);
            }
            
            // Always show seek bar and update progress
            if (recording.getDuration() > 0) {
                int progress = (int) ((recording.getCurrentPosition() * 100.0) / recording.getDuration());
                seekBar.setProgress(progress);
            } else {
                seekBar.setProgress(0);
            }

            // Set up seek bar listener
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && listener != null) {
                        // Calculate position in milliseconds
                        int position = (int) ((progress / 100.0) * recording.getDuration());
                        listener.onSeek(recording, position);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    // Optional: Pause updates while user is dragging
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    // Optional: Resume updates when user stops dragging
                }
            });

            ivPlay.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlayClick(recording);
                }
            });

            // Set play button click on the entire item
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlayClick(recording);
                }
            });

            // Set long press listener on the entire item
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onLongPress(recording);
                    return true;
                }
                return false;
            });
        }
    }
}