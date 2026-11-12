
package com.comp3607.telemetry;

import com.comp3607.telemetry.nalytics.LifecycleChangeDetector;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class LifecycleChangeDetectorTest {

    @Test
    void testClassCreationDetected() {

        LifecycleChangeDetector detector =
                new LifecycleChangeDetector();

        var result = detector.compare(
                Set.of("Student", "Teacher"),
                Set.of("Student", "Teacher", "Employee")
        );

        assertEquals(
                Set.of("Employee"),
                result.added()
        );

        assertTrue(result.removed().isEmpty());
    }

    @Test
    void testClassRemovalDetected() {

        LifecycleChangeDetector detector =
                new LifecycleChangeDetector();

        var result = detector.compare(
                Set.of("Student", "Teacher"),
                Set.of("Student")
        );

        assertEquals(
                Set.of("Teacher"),
                result.removed()
        );
    }

    @Test
    void testNoChanges() {

        LifecycleChangeDetector detector =
                new LifecycleChangeDetector();

        var result = detector.compare(
                Set.of("Student"),
                Set.of("Student")
        );

        assertTrue(result.added().isEmpty());
        assertTrue(result.removed().isEmpty());
    }

    @Test
    void testMultipleChanges() {

        LifecycleChangeDetector detector =
                new LifecycleChangeDetector();

        var result = detector.compare(
                Set.of("A", "B"),
                Set.of("B", "C")
        );

        assertEquals(Set.of("C"), result.added());
        assertEquals(Set.of("A"), result.removed());
    }

    @Test
    void testNullInputsRejected() {

        LifecycleChangeDetector detector =
                new LifecycleChangeDetector();

        assertThrows(
                NullPointerException.class,
                () -> detector.compare(null, Set.of())
        );

        assertThrows(
                NullPointerException.class,
                () -> detector.compare(Set.of(), null)
        );
    }
}
