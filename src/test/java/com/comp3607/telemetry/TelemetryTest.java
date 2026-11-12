
package com.comp3607.telemetry;

import com.comp3607.telemetry.events.TelemetryEvent;
import com.comp3607.telemetry.factory.TelemetryEventFactory;
import com.comp3607.telemetry.repository.TelemetryRepository;
import com.comp3607.telemetry.session.SessionManager;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TelemetryTest {

    // Test 1: Check event creation.
    @Test
    void testEventCreation() {

        TelemetryEvent event = new TelemetryEvent(
                "CLASS_CREATED",
                LocalDateTime.now(),
                "Student class created",
                Map.of("className", "Student")
        );

        assertEquals("CLASS_CREATED", event.getEventType());

        assertEquals(
                "Student",
                event.getDetails().get("className")
        );

        assertNotNull(event.getTimestamp());
    }

    // Test 2: Check the event factory.
    @Test
    void testFactoryCreatesCorrectEvent() {

        TelemetryEventFactory factory =
                new TelemetryEventFactory();

        TelemetryEvent event =
                factory.createCompilationSuccessEvent("Student");

        assertEquals(
                "COMPILATION_SUCCESS",
                event.getEventType()
        );
    }

    // Test 3: Check repository storage.
    @Test
    void testRepositoryStoresEvents() {

        TelemetryRepository repository =
                new TelemetryRepository();

        TelemetryEventFactory factory =
                new TelemetryEventFactory();

        repository.addEvent(
                factory.createClassCreatedEvent("Student")
        );

        assertEquals(1, repository.getEventCount());
    }

    // Test 4: Check filtering by event type.
    @Test
    void testRepositoryFiltersEvents() {

        TelemetryRepository repository =
                new TelemetryRepository();

        TelemetryEventFactory factory =
                new TelemetryEventFactory();

        repository.addEvent(
                factory.createClassCreatedEvent("Student")
        );

        repository.addEvent(
                factory.createCompilationSuccessEvent("Student")
        );

        assertEquals(
                1,
                repository.getEventsByType(
                        "COMPILATION_SUCCESS").size()
        );
    }

    // Test 5: Check session start and end.
    @Test
    void testSessionManagement() {

        TelemetryRepository repository =
                new TelemetryRepository();

        SessionManager session =
                new SessionManager("STUDENT_001", repository);

        session.startSession();

        assertTrue(session.isActive());

        session.endSession();

        assertFalse(session.isActive());
        assertNotNull(session.getEndTime());
    }

    // Test 6: Check compilation counting.
    @Test
    void testCompilationAttemptCounting() {

        TelemetryRepository repository =
                new TelemetryRepository();

        TelemetryEventFactory factory =
                new TelemetryEventFactory();

        SessionManager session =
                new SessionManager("STUDENT_001", repository);

        session.startSession();

        repository.addEvent(
                factory.createCompilationSuccessEvent("Student")
        );

        repository.addEvent(
                factory.createCompilationErrorEvent(
                        "Student",
                        "Missing semicolon",
                        12
                )
        );

        assertEquals(
                2,
                session.getTotalCompilationAttempts()
        );

        session.endSession();
    }
}
