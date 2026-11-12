
package com.comp3607.telemetry.repository;

import com.comp3607.telemetry.events.TelemetryEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Stores and manages telemetry events.
public class TelemetryRepository {

    // Internal list containing all recorded events.
    private final List<TelemetryEvent> events;

    // Constructor: creates an empty repository.
    public TelemetryRepository() {
        events = new ArrayList<>();
    }

    // Adds one telemetry event to the repository.
    public synchronized void addEvent(TelemetryEvent event) {

        Objects.requireNonNull(
                event, "Event cannot be null");

        events.add(event);
    }

    // Returns a separate list of all recorded events.
    public synchronized List<TelemetryEvent> getEvents() {
        return new ArrayList<>(events);
    }

    // Returns the number of recorded events.
    public synchronized int getEventCount() {
        return events.size();
    }

    // Returns all events matching a specified type.
    public synchronized List<TelemetryEvent> getEventsByType(
            String eventType) {

        List<TelemetryEvent> matchingEvents =
                new ArrayList<>();

        for (TelemetryEvent event : events) {

            if (event.getEventType().equals(eventType)) {
                matchingEvents.add(event);
            }
        }

        return matchingEvents;
    }

    // Removes all events from the repository.
    public synchronized void clearEvents() {
        events.clear();
    }
}
