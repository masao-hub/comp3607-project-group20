
package com.comp3607.telemetry.nalytics;

import java.util.Objects;

// Calculates estimated active/idle time and observed edits.
public class CodingActivityTracker {

    // Number of milliseconds before activity becomes idle.
    private final long idleThresholdMillis;

    // Time when this tracker was started.
    private final long startMillis;

    // Last time included in our calculations.
    private long accountedUntilMillis;

    // Time of the most recent detected edit.
    private Long lastEditMillis;

    // Estimated time counters.
    private long activeMillis;
    private long idleMillis;

    // Counts detected editing changes.
    private int editCount;

    // Counts intervals between observed edits.
    private long cadenceTotalMillis;
    private int cadenceIntervalCount;

    // Creates an activity tracker.
    public CodingActivityTracker(
            long startMillis,
            long idleThresholdMillis) {

        if (idleThresholdMillis <= 0) {
            throw new IllegalArgumentException(
                    "Idle threshold must be positive");
        }

        this.startMillis = startMillis;
        this.idleThresholdMillis = idleThresholdMillis;
        this.accountedUntilMillis = startMillis;
    }

    // Accounts for elapsed time since the previous update.
    private void advanceTime(long nowMillis) {

        // Make sure time never moves backwards.
        if (nowMillis < accountedUntilMillis) {
            throw new IllegalArgumentException(
                    "Time cannot move backwards");
        }

        // Calculate the total time since the last update.
        long from = accountedUntilMillis;
        long totalPart = nowMillis - from;

        // Assume all elapsed time is idle until an edit exists.
        long activePart = 0;

        // Only calculate active time if an edit was detected.
        if (lastEditMillis != null) {

            long activeEnd =
                    lastEditMillis + idleThresholdMillis;

            // Calculate the portion inside the active window.
            if (from < activeEnd) {

                activePart = Math.max(
                        0,
                        Math.min(nowMillis, activeEnd) - from
                );
            }
        }

        // Add the estimated active and idle durations.
        activeMillis += activePart;
        idleMillis += totalPart - activePart;

        // Remember how far we've calculated.
        accountedUntilMillis = nowMillis;
    }


    // Called when the editor monitor detects a source change.
    public synchronized void recordEdit(long nowMillis) {

        advanceTime(nowMillis);

        // Measure the interval since the previous detected edit.
        if (lastEditMillis != null) {
            cadenceTotalMillis += nowMillis - lastEditMillis;
            cadenceIntervalCount++;
        }

        lastEditMillis = nowMillis;
        editCount++;
    }

    // Creates a summary up to the supplied time.
    public synchronized ActivitySummary snapshot(
            long nowMillis) {

        advanceTime(nowMillis);

        long durationMillis =
                accountedUntilMillis - startMillis;

        double minutes = durationMillis / 60000.0;

        double editsPerMinute = minutes > 0
                ? editCount / minutes
                : 0.0;

        double averageCadenceSeconds =
                cadenceIntervalCount > 0
                        ? cadenceTotalMillis
                        / (1000.0 * cadenceIntervalCount)
                        : 0.0;

        return new ActivitySummary(
                activeMillis / 1000.0,
                idleMillis / 1000.0,
                durationMillis / 1000.0,
                editCount,
                editsPerMinute,
                averageCadenceSeconds,
                idleThresholdMillis / 1000.0
        );
    }

    // Immutable snapshot of activity statistics.
    public record ActivitySummary(
            double activeSeconds,
            double idleSeconds,
            double observedSeconds,
            int detectedEdits,
            double editsPerMinute,
            double averageObservedEditIntervalSeconds,
            double idleThresholdSeconds) {

        public ActivitySummary {
            if (activeSeconds < 0 || idleSeconds < 0) {
                throw new IllegalArgumentException(
                        "Durations cannot be negative");
            }
        }
    }
}
