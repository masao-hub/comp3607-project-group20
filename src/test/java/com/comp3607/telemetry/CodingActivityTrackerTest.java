
package com.comp3607.telemetry;

import com.comp3607.telemetry.nalytics.CodingActivityTracker;
import com.comp3607.telemetry.nalytics.CodingActivityTracker.ActivitySummary;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CodingActivityTrackerTest {

    // Test 1: No edits means the observed time is idle.
    @Test
    void testNoEditsMeansIdle() {

        CodingActivityTracker tracker =
                new CodingActivityTracker(0, 10_000);

        ActivitySummary result =
                tracker.snapshot(20_000);

        assertEquals(0.0, result.activeSeconds(), 0.001);
        assertEquals(20.0, result.idleSeconds(), 0.001);
        assertEquals(0, result.detectedEdits());
    }

    // Test 2: A detected edit creates an active window.
    @Test
    void testActiveTimeAfterEdit() {

        CodingActivityTracker tracker =
                new CodingActivityTracker(0, 10_000);

        // An edit is detected at 5 seconds.
        tracker.recordEdit(5_000);

        // Check the statistics at 12 seconds.
        ActivitySummary result =
                tracker.snapshot(12_000);

        assertEquals(7.0, result.activeSeconds(), 0.001);
        assertEquals(5.0, result.idleSeconds(), 0.001);
        assertEquals(1, result.detectedEdits());
    }

    // Test 3: Active time ends after the idle threshold.
    @Test
    void testIdleTimeAfterThreshold() {

        CodingActivityTracker tracker =
                new CodingActivityTracker(0, 10_000);

        tracker.recordEdit(5_000);

        ActivitySummary result =
                tracker.snapshot(20_000);

        assertEquals(10.0, result.activeSeconds(), 0.001);
        assertEquals(10.0, result.idleSeconds(), 0.001);
        assertEquals(20.0, result.observedSeconds(), 0.001);
    }

    // Test 4: Multiple edits extend the active window.
    @Test
    void testMultipleEdits() {

        CodingActivityTracker tracker =
                new CodingActivityTracker(0, 10_000);

        tracker.recordEdit(5_000);
        tracker.recordEdit(10_000);

        ActivitySummary result =
                tracker.snapshot(25_000);

        assertEquals(15.0, result.activeSeconds(), 0.001);
        assertEquals(10.0, result.idleSeconds(), 0.001);
        assertEquals(2, result.detectedEdits());
    }

    // Test 5: Calculate editing frequency.
    @Test
    void testEditingFrequency() {

        CodingActivityTracker tracker =
                new CodingActivityTracker(0, 10_000);

        tracker.recordEdit(5_000);
        tracker.recordEdit(15_000);
        tracker.recordEdit(25_000);

        ActivitySummary result =
                tracker.snapshot(60_000);

        // Three observed changes in one minute.
        assertEquals(
                3.0,
                result.editsPerMinute(),
                0.001
        );

        // The average gap between observed edits is 10s.
        assertEquals(
                10.0,
                result.averageObservedEditIntervalSeconds(),
                0.001
        );
    }

    // Test 6: Invalid idle threshold must be rejected.
    @Test
    void testInvalidIdleThreshold() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new CodingActivityTracker(0, 0)
        );
    }

    // Test 7: Time must not move backwards.
    @Test
    void testTimeCannotMoveBackwards() {

        CodingActivityTracker tracker =
                new CodingActivityTracker(10_000, 10_000);

        assertThrows(
                IllegalArgumentException.class,
                () -> tracker.snapshot(5_000)
        );
    }

    // Test 8: Snapshots should not double-count time.
    @Test
    void testRepeatedSnapshots() {

        CodingActivityTracker tracker =
                new CodingActivityTracker(0, 10_000);

        tracker.recordEdit(5_000);

        tracker.snapshot(10_000);

        ActivitySummary result =
                tracker.snapshot(20_000);

        assertEquals(10.0, result.activeSeconds(), 0.001);
        assertEquals(10.0, result.idleSeconds(), 0.001);
    }

    // Test 9: Active plus idle should equal observed time.
    @Test
    void testTimeTotals() {

        CodingActivityTracker tracker =
                new CodingActivityTracker(0, 10_000);

        tracker.recordEdit(4_000);
        tracker.recordEdit(8_000);

        ActivitySummary result =
                tracker.snapshot(30_000);

        assertEquals(
                result.observedSeconds(),
                result.activeSeconds() + result.idleSeconds(),
                0.001
        );
    }
}
