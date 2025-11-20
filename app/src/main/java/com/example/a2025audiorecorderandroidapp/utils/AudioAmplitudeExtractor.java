package com.example.a2025audiorecorderandroidapp.utils;

public class AudioAmplitudeExtractor {

    /**
     * Calculate RMS amplitude from PCM byte array
     * @param pcmData Raw PCM audio data
     * @param sampleRate Sample rate of the audio
     * @param channelCount Number of channels (1 for mono, 2 for stereo)
     * @return Normalized amplitude value between 0.0 and 1.0
     */
    public static float calculateRMSAmplitude(byte[] pcmData, int sampleRate, int channelCount) {
        if (pcmData == null || pcmData.length == 0) {
            return 0.0f;
        }

        // Assuming 16-bit PCM
        int bytesPerSample = 2; // 16-bit = 2 bytes per sample
        int samplesToProcess = Math.min(pcmData.length / bytesPerSample, sampleRate / 10); // Process ~100ms of audio

        double sum = 0.0;
        int sampleCount = 0;

        for (int i = 0; i < samplesToProcess * bytesPerSample && i < pcmData.length - 1; i += bytesPerSample) {
            // Convert bytes to 16-bit sample
            short sample = (short) ((pcmData[i] & 0xFF) | (pcmData[i + 1] << 8));

            // Convert to float and normalize
            float normalizedSample = sample / 32768.0f;

            sum += normalizedSample * normalizedSample;
            sampleCount++;
        }

        if (sampleCount == 0) {
            return 0.0f;
        }

        // Calculate RMS
        double rms = Math.sqrt(sum / sampleCount);

        // Normalize to 0-1 range (adjust multiplier based on your audio levels)
        return (float) Math.min(1.0, rms * 2.0); // Multiplier can be adjusted
    }

    /**
     * Calculate peak amplitude from PCM byte array
     * @param pcmData Raw PCM audio data
     * @param sampleRate Sample rate of the audio
     * @param channelCount Number of channels
     * @return Normalized amplitude value between 0.0 and 1.0
     */
    public static float calculatePeakAmplitude(byte[] pcmData, int sampleRate, int channelCount) {
        if (pcmData == null || pcmData.length == 0) {
            return 0.0f;
        }

        // Assuming 16-bit PCM
        int bytesPerSample = 2;
        int samplesToProcess = Math.min(pcmData.length / bytesPerSample, sampleRate / 10);

        float maxAmplitude = 0.0f;

        for (int i = 0; i < samplesToProcess * bytesPerSample && i < pcmData.length - 1; i += bytesPerSample) {
            short sample = (short) ((pcmData[i] & 0xFF) | (pcmData[i + 1] << 8));
            float normalizedSample = Math.abs(sample) / 32768.0f;
            maxAmplitude = Math.max(maxAmplitude, normalizedSample);
        }

        return maxAmplitude;
    }

    /**
     * Extract multiple amplitude values from a larger PCM buffer
     * Useful for creating smooth waveform animations
     * @param pcmData Raw PCM audio data
     * @param sampleRate Sample rate
     * @param channelCount Number of channels
     * @param chunkSize Size of each chunk to process (in samples)
     * @return Array of normalized amplitude values
     */
    public static float[] extractAmplitudes(byte[] pcmData, int sampleRate, int channelCount, int chunkSize) {
        if (pcmData == null || pcmData.length == 0) {
            return new float[0];
        }

        int bytesPerSample = 2;
        int totalSamples = pcmData.length / bytesPerSample;
        int numChunks = totalSamples / chunkSize;

        if (numChunks == 0) {
            return new float[]{calculateRMSAmplitude(pcmData, sampleRate, channelCount)};
        }

        float[] amplitudes = new float[numChunks];

        for (int i = 0; i < numChunks; i++) {
            int startByte = i * chunkSize * bytesPerSample;
            int endByte = Math.min(startByte + chunkSize * bytesPerSample, pcmData.length);

            byte[] chunk = new byte[endByte - startByte];
            System.arraycopy(pcmData, startByte, chunk, 0, chunk.length);

            amplitudes[i] = calculateRMSAmplitude(chunk, sampleRate, channelCount);
        }

        return amplitudes;
    }
}