
package com.comp3607.telemetry.exporters;

import com.comp3607.telemetry.events.TelemetryEvent;
import com.comp3607.telemetry.repository.TelemetryRepository;
import com.comp3607.telemetry.session.SessionManager;
import com.comp3607.telemetry.nalytics.CodingActivityTracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// Converts recorded telemetry data into a JSON file.
public class JsonExporter {

    // Gson is responsible for formatting the JSON text.
    private final Gson gson;

    // Constructor: configures readable JSON output.
    public JsonExporter() {
        gson = new GsonBuilder()
                .serializeNulls()
                .setPrettyPrinting()
                .create();
    }


    // Converts a date/time into text.
    private String formatTime(LocalDateTime time) {

        if (time == null) {
            return null;
        }

        return time.format(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
        );
    }

    // Converts one TelemetryEvent into a JSON object.
    private JsonObject convertEvent(TelemetryEvent event) {

        JsonObject object = new JsonObject();

        object.addProperty(
                "eventType",
                event.getEventType()
        );

        object.addProperty(
                "timestamp",
                formatTime(event.getTimestamp())
        );

        object.addProperty(
                "description",
                event.getDescription()
        );

        JsonObject details = new JsonObject();

        for (Map.Entry<String, String> entry
                : event.getDetails().entrySet()) {

            details.addProperty(
                    entry.getKey(),
                    entry.getValue()
            );
        }

        object.add("details", details);

        return object;
    }

    // Builds the complete JSON document in memory.
    public String toJson(
            SessionManager session,
            TelemetryRepository repository) {

        Objects.requireNonNull(session);
        Objects.requireNonNull(repository);

        // Get a snapshot of the recorded events.
        List<TelemetryEvent> events =
                repository.getEvents();

        JsonObject root = new JsonObject();

        // Describes the version of our JSON structure.
        root.addProperty("schemaVersion", "1.0");

        root.addProperty(
                "pluginName",
                "BJTrace"
        );

        root.addProperty(
                "exportedAt",
                formatTime(LocalDateTime.now())
        );

        root.addProperty(
                "timeZone",
                ZoneId.systemDefault().getId()
        );

        // Create session metadata.
        JsonObject metadata = new JsonObject();

        metadata.addProperty(
                "studentId",
                session.getStudentId()
        );

        metadata.addProperty(
                "sessionStart",
                formatTime(session.getStartTime())
        );

        metadata.addProperty(
                "sessionEnd",
                formatTime(session.getEndTime())
        );

        metadata.addProperty(
                "sessionActive",
                session.isActive()
        );

        metadata.addProperty(
                "sessionDurationSeconds",
                session.getSessionDurationSeconds()
        );

        // Count compilation outcomes.
        int successful = 0;
        int failed = 0;

        for (TelemetryEvent event : events) {

            if ("COMPILATION_SUCCESS".equals(
                    event.getEventType())) {

                successful++;

            } else if ("COMPILATION_ERROR".equals(
                    event.getEventType())) {

                failed++;
            }
        }

        metadata.addProperty(
                "totalCompilationAttempts",
                successful + failed
        );

        metadata.addProperty(
                "successfulCompilations",
                successful
        );

        metadata.addProperty(
                "failedCompilations",
                failed
        );

        metadata.addProperty(
                "totalEvents",
                events.size()
        );

        root.add("sessionMetadata", metadata);

        // Convert all telemetry events into a JSON array.
        JsonArray eventArray = new JsonArray();

        for (TelemetryEvent event : events) {
            eventArray.add(convertEvent(event));
        }

        root.add("events", eventArray);

        // Convert the JSON objects into readable text.
        return gson.toJson(root);
    }

    // Saves the JSON text to a file on the computer.
    public Path export(
            SessionManager session,
            TelemetryRepository repository,
            Path outputFile) throws IOException {

        Objects.requireNonNull(outputFile);

        String jsonText = toJson(
                session,
                repository
        );

        // Create the parent folder if necessary.
        Path absolutePath =
                outputFile.toAbsolutePath();

        Path parent = absolutePath.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        // Write the JSON using UTF-8 encoding.
        Files.writeString(
                absolutePath,
                jsonText,
                StandardCharsets.UTF_8
        );

        return absolutePath;
    }

    // Builds JSON including estimated coding activity.
    public String toJson(
            SessionManager session,
            TelemetryRepository repository,
            CodingActivityTracker tracker) {

        Objects.requireNonNull(tracker);

        // Start with the JSON created by our original method.
        String originalJson = toJson(session, repository);

        JsonObject root = com.google.gson.JsonParser
                .parseString(originalJson)
                .getAsJsonObject();

        // Calculate activity statistics up to this moment.
        CodingActivityTracker.ActivitySummary summary =
                tracker.snapshot(System.currentTimeMillis());

        JsonObject activity = new JsonObject();

        activity.addProperty(
                "measurementMethod",
                "SOURCE_SNAPSHOT"
        );

        activity.addProperty(
                "activeTimeSeconds",
                summary.activeSeconds()
        );

        activity.addProperty(
                "idleTimeSeconds",
                summary.idleSeconds()
        );

        activity.addProperty(
                "observedDurationSeconds",
                summary.observedSeconds()
        );

        activity.addProperty(
                "detectedEdits",
                summary.detectedEdits()
        );

        activity.addProperty(
                "editsPerMinute",
                summary.editsPerMinute()
        );

        activity.addProperty(
                "averageObservedEditIntervalSeconds",
                summary.averageObservedEditIntervalSeconds()
        );

        activity.addProperty(
                "idleThresholdSeconds",
                summary.idleThresholdSeconds()
        );

        root.add("codingActivity", activity);

        return gson.toJson(root);
    }

    // Writes JSON with activity statistics to a file.
    public Path export(
            SessionManager session,
            TelemetryRepository repository,
            CodingActivityTracker tracker,
            Path outputFile) throws IOException {

        Objects.requireNonNull(outputFile);

        String jsonText = toJson(
                session,
                repository,
                tracker
        );

        Path absolutePath = outputFile.toAbsolutePath();
        Path parent = absolutePath.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.writeString(
                absolutePath,
                jsonText,
                StandardCharsets.UTF_8
        );

        return absolutePath;
    }
}
