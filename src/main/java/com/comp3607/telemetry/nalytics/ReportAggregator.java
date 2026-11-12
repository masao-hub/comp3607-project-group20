
package com.comp3607.telemetry.nalytics;

import com.comp3607.telemetry.events.TelemetryEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// Calculates statistics for telemetry reports.
public class ReportAggregator {

    // Holds all calculated report statistics.
    public record ReportData(
            int totalEvents,
            int compilationAttempts,
            int successfulCompilations,
            int failedCompilations,
            double compilationSuccessRate,
            Map<String, Integer> errorFrequencies,
            List<TelemetryEvent> timeline,
            int runtimeExceptions
    ) {}

    // Converts collected events into report statistics.
    public ReportData aggregate(List<TelemetryEvent> events) {

        Objects.requireNonNull(
                events, "Events cannot be null"
        );

        int successful = 0;
        int failed = 0;
        int runtimeExceptions = 0;

        Map<String, Integer> errors =
                new LinkedHashMap<>();

        List<TelemetryEvent> timeline =
                new ArrayList<>();

        // Examine every recorded telemetry event.
        for (TelemetryEvent event : events) {

            String type = event.getEventType();

            // Count successful compilation attempts.
            if ("COMPILATION_SUCCESS".equals(type)) {
                successful++;
            }

            // Count failed compilation attempts.
            if ("COMPILATION_ERROR".equals(type)) {
                failed++;
            }

            // Count runtime exception terminations.
            if ("RUNTIME_EXCEPTION_DETECTED".equals(type)) {
                runtimeExceptions++;
            }

            // Count individual compiler diagnostic messages.
            if ("COMPILATION_DIAGNOSTIC".equals(type)) {

                String message = event.getDetails()
                        .getOrDefault(
                                "errorMessage",
                                "Unknown compilation error"
                        );

                errors.merge(
                        message,
                        1,
                        Integer::sum
                );
            }

            // Include relevant events in the timeline.
            if (isTimelineEvent(type)) {
                timeline.add(event);
            }
        }

        // Calculate the total number of compilations.
        int attempts = successful + failed;

        // Avoid division by zero.
        double successRate = attempts == 0
                ? 0.0
                : 100.0 * successful / attempts;

        // Sort events from earliest to latest.
        timeline.sort(
                Comparator.comparing(
                        TelemetryEvent::getTimestamp
                )
        );

        // Sort error messages by their frequency.
        Map<String, Integer> sortedErrors =
                new LinkedHashMap<>();

        errors.entrySet().stream()
                .sorted(
                        Map.Entry.<String, Integer>
                                        comparingByValue()
                                .reversed()
                                .thenComparing(
                                        Map.Entry.comparingByKey()
                                )
                )
                .forEach(entry ->
                        sortedErrors.put(
                                entry.getKey(),
                                entry.getValue()
                        )
                );

        // Return a single object containing all statistics.
        return new ReportData(
                events.size(),
                attempts,
                successful,
                failed,
                successRate,
                Collections.unmodifiableMap(sortedErrors),
                List.copyOf(timeline),
                runtimeExceptions
        );
    }

    // Determines which events appear in the PDF timeline.
    private boolean isTimelineEvent(String type) {

        return switch (type) {

            case "PROJECT_OPENED_DETECTED",
                 "PROJECT_CLOSED_DETECTED",
                 "PACKAGE_OPENED",
                 "PACKAGE_CLOSING",
                 "CLASS_CREATED",
                 "CLASS_CREATED_DETECTED",
                 "CLASS_RENAMED",
                 "CLASS_REMOVED",
                 "CLASS_STATE_CHANGED",
                 "METHOD_DEFINED",
                 "METHOD_OBSERVED",
                 "COMPILATION_SUCCESS",
                 "COMPILATION_ERROR",
                 "INVOCATION_FINISHED",
                 "RUNTIME_EXCEPTION_DETECTED",
                 "EXECUTION_TERMINATED" -> true;

            default -> false;
        };
    }
}
