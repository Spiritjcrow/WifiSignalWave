package com.wifisignal.wave.sensor;

/** Stateful one-dimensional RSSI filter; percentage is an estimated display heuristic. */
public final class SignalProcessor {
    private static final float MEASUREMENT_NOISE = 5f;
    private static final float PROCESS_NOISE = 0.5f;
    private float estimate;
    private float error;
    private boolean initialized;

    public SignalProcessor() { reset(); }

    public void reset() {
        estimate = -65f;
        error = 10f;
        initialized = false;
    }

    public float filterRSSI(float measurement) {
        if (!initialized) {
            estimate = measurement;
            error = MEASUREMENT_NOISE;
            initialized = true;
            return estimate;
        }
        float predictedError = error + PROCESS_NOISE;
        float gain = predictedError / (predictedError + MEASUREMENT_NOISE);
        estimate += gain * (measurement - estimate);
        error = (1f - gain) * predictedError;
        return estimate;
    }

    public int getSignalQuality(float rssi) {
        if (rssi >= -50f) return 100;
        if (rssi <= -100f) return 0;
        return Math.round((rssi + 100f) * 2f);
    }
}
