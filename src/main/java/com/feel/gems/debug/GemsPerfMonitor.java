package com.feel.gems.debug;

import java.util.Arrays;

/**
 * Server tick-time monitor for perf regression checks (MSPT stats).
 * Records tick durations on the server thread (nanoTime) into a ring buffer.
 */
public final class GemsPerfMonitor {
    private static final int MAX_SAMPLES = 2400; // 2 minutes at 20 TPS

    private static final long[] SAMPLES_NANOS = new long[MAX_SAMPLES];
    private static int writeIndex = 0;
    private static int count = 0;

    private static long tickStartNanos = 0L;

    private GemsPerfMonitor() {
    }

    public static void onTickStart() {
        tickStartNanos = System.nanoTime();
    }

    public static void onTickEnd() {
        long start = tickStartNanos;
        if (start == 0L) {
            return;
        }
        long delta = System.nanoTime() - start;
        if (delta <= 0L) {
            return;
        }
        SAMPLES_NANOS[writeIndex] = delta;
        writeIndex = (writeIndex + 1) % MAX_SAMPLES;
        if (count < MAX_SAMPLES) {
            count++;
        }
    }

    public static void reset() {
        Arrays.fill(SAMPLES_NANOS, 0L);
        writeIndex = 0;
        count = 0;
        tickStartNanos = 0L;
    }

    public static Snapshot snapshot(int windowTicks) {
        int n = count;
        if (n <= 0) {
            return new Snapshot(0, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        int window = clamp(windowTicks, 1, n);

        long[] copy = new long[window];
        long sum = 0L;
        long max = 0L;

        int idx = writeIndex - 1;
        if (idx < 0) {
            idx += MAX_SAMPLES;
        }
        for (int i = 0; i < window; i++) {
            long v = SAMPLES_NANOS[idx];
            copy[i] = v;
            sum += v;
            if (v > max) {
                max = v;
            }
            idx--;
            if (idx < 0) {
                idx += MAX_SAMPLES;
            }
        }

        Arrays.sort(copy);
        double avgMs = nanosToMs(sum / (double) window);
        double medianMs = nanosToMs(copy[window / 2]);
        double p95Ms = nanosToMs(copy[(int) Math.min(window - 1, Math.floor(window * 0.95D))]);
        double maxMs = nanosToMs(max);
        return new Snapshot(window, avgMs, medianMs, p95Ms, maxMs);
    }

    private static double nanosToMs(double nanos) {
        return nanos / 1_000_000.0D;
    }

    private static int clamp(int v, int min, int max) {
        if (v < min) {
            return min;
        }
        if (v > max) {
            return max;
        }
        return v;
    }

    public record Snapshot(int samples, double avgMspt, double medianMspt, double p95Mspt, double maxMspt) {
    }
}
