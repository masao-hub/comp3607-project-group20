
package com.comp3607.telemetry.factory;

import com.comp3607.telemetry.events.TelemetryEvent;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

// Factory responsible for creating telemetry events.
public class TelemetryEventFactory {

    // Creates a general-purpose telemetry event.
    public TelemetryEvent createEvent(
            String eventType,
            String description,
            Map<String, String> details) {

        return new TelemetryEvent(
                eventType,
                LocalDateTime.now(),
                description,
                details
        );
    }

    // Creates an event when a student creates a class.
    public TelemetryEvent createClassCreatedEvent(
            String className) {

        Objects.requireNonNull(className);

        return createEvent(
                "CLASS_CREATED",
                "Class " + className + " was created",
                Map.of("className", className)
        );
    }

    // Creates an event when a class compiles successfully.
    public TelemetryEvent createCompilationSuccessEvent(
            String className) {

        Objects.requireNonNull(className);

        return createEvent(
                "COMPILATION_SUCCESS",
                "Class " + className
                        + " compiled successfully",
                Map.of("className", className)
        );
    }

    // Creates an event when compilation fails.
    public TelemetryEvent createCompilationErrorEvent(
            String className,
            String errorMessage,
            int lineNumber) {

        Objects.requireNonNull(className);
        Objects.requireNonNull(errorMessage);

        if (lineNumber < 0) {
            throw new IllegalArgumentException(
                    "Line number cannot be negative");
        }

        return createEvent(
                "COMPILATION_ERROR",
                "Compilation failed for " + className,
                Map.of(
                        "className", className,
                        "errorMessage", errorMessage,
                        "lineNumber", String.valueOf(lineNumber)
                )
        );
    }

    // Creates an event when a program is executed.
    public TelemetryEvent createExecutionEvent(
            String className) {

        Objects.requireNonNull(className);

        return createEvent(
                "PROGRAM_EXECUTED",
                "Program executed from " + className,
                Map.of("className", className)
        );
    }
}
