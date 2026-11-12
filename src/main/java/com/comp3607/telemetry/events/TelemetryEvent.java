
package com.comp3607.telemetry.events;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

// Represents one student activity recorded by the plugin.
public class TelemetryEvent {

    // The kind of activity being recorded.
    private final String eventType;

    // The date and time when the activity happened.
    private final LocalDateTime timestamp;

    // A short explanation of the activity.
    private final String description;

    // Extra information about the activity.
    private final Map<String, String> details;

    // Constructor: creates a new telemetry event.
    public TelemetryEvent(
            String eventType,
            LocalDateTime timestamp,
            String description,
            Map<String, String> details) {

        this.eventType = Objects.requireNonNull(
                eventType, "Event type cannot be null");

        this.timestamp = Objects.requireNonNull(
                timestamp, "Timestamp cannot be null");

        this.description = Objects.requireNonNull(
                description, "Description cannot be null");

        this.details = Collections.unmodifiableMap(
                new LinkedHashMap<>(
                        Objects.requireNonNull(
                                details, "Details cannot be null")));
    }

    // Returns the type of event.
    public String getEventType() {
        return eventType;
    }

    // Returns the timestamp.
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    // Returns the description.
    public String getDescription() {
        return description;
    }

    // Returns the extra event details.
    public Map<String, String> getDetails() {
        return details;
    }

    // Provides a readable representation of the event.
    @Override
    public String toString() {
        return "TelemetryEvent{" +
                "eventType='" + eventType + '\'' +
                ", timestamp=" + timestamp +
                ", description='" + description + '\'' +
                ", details=" + details +
                '}';
    }
}
