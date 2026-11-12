
package com.comp3607.telemetry;

import com.comp3607.telemetry.exporters.JsonExporter;
import com.comp3607.telemetry.factory.TelemetryEventFactory;
import com.comp3607.telemetry.repository.TelemetryRepository;
import com.comp3607.telemetry.session.SessionManager;
import com.comp3607.telemetry.nalytics.CodingActivityTracker;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class JsonExporterTest {

    private TelemetryRepository repository;
    private TelemetryEventFactory factory;
    private SessionManager session;
    private JsonExporter exporter;

    // JUnit creates a temporary directory for file tests.
    @TempDir
    Path temporaryFolder;

    // Runs before each individual test.
    @BeforeEach
    void setUp() {

        repository = new TelemetryRepository();
        factory = new TelemetryEventFactory();

        session = new SessionManager(
                "STUDENT_001",
                repository
        );

        exporter = new JsonExporter();

        session.startSession();
    }

    // Test 1: Output must be valid JSON.
    @Test
    void testProducesValidJson() {

        String json = exporter.toJson(
                session,
                repository
        );

        JsonObject root = JsonParser
                .parseString(json)
                .getAsJsonObject();

        assertEquals(
                "1.0",
                root.get("schemaVersion").getAsString()
        );

        assertEquals(
                "BJTrace",
                root.get("pluginName").getAsString()
        );

        assertTrue(root.get("events").isJsonArray());
    }

    // Test 2: Session metadata must be present.
    @Test
    void testSessionMetadata() {

        String json = exporter.toJson(
                session,
                repository
        );

        JsonObject root = JsonParser
                .parseString(json)
                .getAsJsonObject();

        JsonObject metadata = root
                .getAsJsonObject("sessionMetadata");

        assertEquals(
                "STUDENT_001",
                metadata.get("studentId").getAsString()
        );

        assertTrue(
                metadata.get("sessionActive").getAsBoolean()
        );

        assertNotNull(metadata.get("sessionStart"));

        assertTrue(metadata.get("sessionEnd").isJsonNull());
    }

    // Test 3: Compilation totals must be correct.
    @Test
    void testCompilationStatistics() {

        repository.addEvent(
                factory.createCompilationSuccessEvent(
                        "Student.java"
                )
        );

        repository.addEvent(
                factory.createCompilationErrorEvent(
                        "Student.java",
                        "Missing semicolon",
                        12
                )
        );

        JsonObject metadata = JsonParser
                .parseString(
                        exporter.toJson(session, repository)
                )
                .getAsJsonObject()
                .getAsJsonObject("sessionMetadata");

        assertEquals(
                2,
                metadata.get("totalCompilationAttempts")
                        .getAsInt()
        );

        assertEquals(
                1,
                metadata.get("successfulCompilations")
                        .getAsInt()
        );

        assertEquals(
                1,
                metadata.get("failedCompilations")
                        .getAsInt()
        );
    }

    // Test 4: Event details must be preserved.
    @Test
    void testEventDetails() {

        repository.addEvent(
                factory.createCompilationErrorEvent(
                        "Student.java",
                        "Missing semicolon",
                        12
                )
        );

        JsonObject root = JsonParser
                .parseString(
                        exporter.toJson(session, repository)
                )
                .getAsJsonObject();

        JsonObject event = root
                .getAsJsonArray("events")
                .get(0)
                .getAsJsonObject();

        assertEquals(
                "COMPILATION_ERROR",
                event.get("eventType").getAsString()
        );

        JsonObject details =
                event.getAsJsonObject("details");

        assertEquals(
                "Missing semicolon",
                details.get("errorMessage").getAsString()
        );

        assertEquals(
                "12",
                details.get("lineNumber").getAsString()
        );
    }

    // Test 5: A JSON file must be created on disk.
    @Test
    void testWritesJsonFile() throws IOException {

        repository.addEvent(
                factory.createClassCreatedEvent("Student")
        );

        Path outputFile = temporaryFolder
                .resolve("reports")
                .resolve("telemetry-test.json");

        Path savedFile = exporter.export(
                session,
                repository,
                outputFile
        );

        assertTrue(Files.exists(savedFile));

        String fileContents = Files.readString(
                savedFile,
                StandardCharsets.UTF_8
        );

        JsonObject root = JsonParser
                .parseString(fileContents)
                .getAsJsonObject();

        assertEquals(
                1,
                root.getAsJsonArray("events").size()
        );

        assertEquals(
                1,
                root.getAsJsonObject("sessionMetadata")
                        .get("totalEvents")
                        .getAsInt()
        );
    }

    @Test
    void testJsonContainsCodingActivity() {

        // Use a recent timestamp for compatibility
        // with the exporter's current-time snapshot.
        long start = System.currentTimeMillis() - 20_000;

        CodingActivityTracker activityTracker =
                new CodingActivityTracker(start, 10_000);

        activityTracker.recordEdit(start + 5_000);

        String json = exporter.toJson(
                session,
                repository,
                activityTracker
        );

        JsonObject root = JsonParser
                .parseString(json)
                .getAsJsonObject();

        JsonObject activity =
                root.getAsJsonObject("codingActivity");

        assertNotNull(activity);

        assertEquals(
                "SOURCE_SNAPSHOT",
                activity.get("measurementMethod").getAsString()
        );

        assertEquals(
                1,
                activity.get("detectedEdits").getAsInt()
        );

        assertTrue(
                activity.get("observedDurationSeconds")
                        .getAsDouble() >= 20.0
        );

        assertTrue(
                activity.get("activeTimeSeconds")
                        .getAsDouble() >= 0.0
        );
    }
}
