
package com.comp3607.telemetry;

import com.comp3607.telemetry.events.TelemetryEvent;
import com.comp3607.telemetry.factory.TelemetryEventFactory;
import com.comp3607.telemetry.nalytics.ReportAggregator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ReportAggregatorTest {

    private ReportAggregator aggregator;
    private TelemetryEventFactory factory;
    private List<TelemetryEvent> events;

    @BeforeEach
    void setUp() {

        aggregator = new ReportAggregator();
        factory = new TelemetryEventFactory();
        events = new ArrayList<>();
    }

    // Test 1: Empty telemetry produces zero attempts.
    @Test
    void testEmptyEvents() {

        ReportAggregator.ReportData result =
                aggregator.aggregate(events);

        assertEquals(0, result.totalEvents());
        assertEquals(0, result.compilationAttempts());
        assertEquals(0.0,
                result.compilationSuccessRate(), 0.001);
    }

    // Test 2: Successful and failed attempts are counted.
    @Test
    void testCompilationCounts() {

        events.add(
                factory.createCompilationSuccessEvent("Student")
        );

        events.add(
                factory.createCompilationSuccessEvent("Student")
        );

        events.add(
                factory.createCompilationErrorEvent(
                        "Student",
                        "Compilation failed",
                        5
                )
        );

        ReportAggregator.ReportData result =
                aggregator.aggregate(events);

        assertEquals(3, result.compilationAttempts());
        assertEquals(2, result.successfulCompilations());
        assertEquals(1, result.failedCompilations());
    }

    // Test 3: Success rate is mathematically correct.
    @Test
    void testSuccessRate() {

        for (int i = 0; i < 8; i++) {
            events.add(
                    factory.createCompilationSuccessEvent("Student")
            );
        }

        for (int i = 0; i < 2; i++) {
            events.add(
                    factory.createCompilationErrorEvent(
                            "Student",
                            "Error",
                            3
                    )
            );
        }

        ReportAggregator.ReportData result =
                aggregator.aggregate(events);

        assertEquals(
                80.0,
                result.compilationSuccessRate(),
                0.001
        );
    }

    // Test 4: Compiler diagnostic messages are grouped.
    @Test
    void testErrorFrequencies() {

        events.add(factory.createEvent(
                "COMPILATION_DIAGNOSTIC",
                "Compiler diagnostic",
                Map.of("errorMessage", "';' expected")
        ));

        events.add(factory.createEvent(
                "COMPILATION_DIAGNOSTIC",
                "Compiler diagnostic",
                Map.of("errorMessage", "';' expected")
        ));

        events.add(factory.createEvent(
                "COMPILATION_DIAGNOSTIC",
                "Compiler diagnostic",
                Map.of("errorMessage", "Cannot find symbol")
        ));

        ReportAggregator.ReportData result =
                aggregator.aggregate(events);

        assertEquals(
                2,
                result.errorFrequencies().get("';' expected")
        );

        assertEquals(
                1,
                result.errorFrequencies().get("Cannot find symbol")
        );
    }

    // Test 5: Timeline events are sorted by timestamp.
    @Test
    void testTimelineOrder() {

        LocalDateTime start =
                LocalDateTime.of(2026, 11, 11, 10, 0);

        TelemetryEvent later = new TelemetryEvent(
                "CLASS_CREATED",
                start.plusMinutes(10),
                "Class B created",
                Map.of("className", "B")
        );

        TelemetryEvent earlier = new TelemetryEvent(
                "CLASS_CREATED",
                start,
                "Class A created",
                Map.of("className", "A")
        );

        events.add(later);
        events.add(earlier);

        ReportAggregator.ReportData result =
                aggregator.aggregate(events);

        assertEquals(
                "Class A created",
                result.timeline().get(0).getDescription()
        );

        assertEquals(
                "Class B created",
                result.timeline().get(1).getDescription()
        );
    }

    // Test 6: Editing events are excluded from this timeline.
    @Test
    void testTimelineFiltering() {

        events.add(factory.createEvent(
                "EDITOR_CHANGE_DETECTED",
                "Source changed",
                Map.of("className", "Student")
        ));

        events.add(factory.createClassCreatedEvent("Student"));

        ReportAggregator.ReportData result =
                aggregator.aggregate(events);

        assertEquals(1, result.timeline().size());

        assertEquals(
                "CLASS_CREATED",
                result.timeline().get(0).getEventType()
        );
    }

    @Test
    void testRuntimeExceptionCounting() {

        events.add(factory.createEvent(
                "INVOCATION_FINISHED",
                "Method execution finished",
                Map.of("outcome", "NORMAL_EXIT")
        ));

        events.add(factory.createEvent(
                "RUNTIME_EXCEPTION_DETECTED",
                "Method ended with exception",
                Map.of("outcome", "EXCEPTION_EXIT")
        ));

        ReportAggregator.ReportData result =
                aggregator.aggregate(events);

        assertEquals(1, result.runtimeExceptions());
    }
}

