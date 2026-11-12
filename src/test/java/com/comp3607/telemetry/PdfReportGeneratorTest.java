
package com.comp3607.telemetry;

import com.comp3607.telemetry.events.TelemetryEvent;
import com.comp3607.telemetry.factory.TelemetryEventFactory;
import com.comp3607.telemetry.nalytics.CodingActivityTracker;
import com.comp3607.telemetry.reports.PdfReportGenerator;
import com.comp3607.telemetry.repository.TelemetryRepository;
import com.comp3607.telemetry.session.SessionManager;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PdfReportGeneratorTest {

    @TempDir
    Path tempDir;

    private TelemetryRepository repository;
    private SessionManager session;
    private TelemetryEventFactory factory;
    private PdfReportGenerator generator;

    @BeforeEach
    void setUp() {

        repository = new TelemetryRepository();

        session = new SessionManager(
                "STUDENT_001",
                repository
        );

        factory = new TelemetryEventFactory();
        generator = new PdfReportGenerator();

        session.startSession();
    }

    // Creates a predictable activity summary for testing.
    private CodingActivityTracker.ActivitySummary activity() {

        return new CodingActivityTracker.ActivitySummary(
                20.0,
                10.0,
                30.0,
                3,
                6.0,
                4.0,
                10.0
        );
    }

    // Test 1: A valid PDF file must be created.
    @Test
    void testPdfFileCreated() throws IOException {

        Path output = tempDir.resolve("session.pdf");

        Path result = generator.generate(
                session,
                repository.getEvents(),
                activity(),
                output
        );

        assertTrue(Files.exists(result));
        assertTrue(Files.size(result) > 0);

        try (PDDocument document =
                     Loader.loadPDF(result.toFile())) {

            assertTrue(document.getNumberOfPages() >= 1);
        }
    }

    // Test 2: Report must contain student metadata.
    @Test
    void testPdfContainsStudentInformation()
            throws IOException {

        Path output = tempDir.resolve("student.pdf");

        generator.generate(
                session,
                repository.getEvents(),
                activity(),
                output
        );

        try (PDDocument document =
                     Loader.loadPDF(output.toFile())) {

            String text = new PDFTextStripper()
                    .getText(document);

            assertTrue(text.contains("STUDENT_001"));
            assertTrue(text.contains("SESSION OVERVIEW"));
        }
    }

    // Test 3: Compilation statistics must appear.
    @Test
    void testPdfContainsCompilationStatistics()
            throws IOException {

        repository.addEvent(
                factory.createCompilationSuccessEvent("Student")
        );

        repository.addEvent(
                factory.createCompilationErrorEvent(
                        "Student",
                        "Missing semicolon",
                        5
                )
        );

        Path output = tempDir.resolve("compilations.pdf");

        generator.generate(
                session,
                repository.getEvents(),
                activity(),
                output
        );

        try (PDDocument document =
                     Loader.loadPDF(output.toFile())) {

            String text = new PDFTextStripper()
                    .getText(document);

            assertTrue(text.contains(
                    "Compilation attempts: 2"
            ));

            assertTrue(text.contains(
                    "Success rate: 50.0%"
            ));
        }
    }

    // Test 4: Coding activity totals must appear.
    @Test
    void testPdfContainsActivityStatistics()
            throws IOException {

        Path output = tempDir.resolve("activity.pdf");

        generator.generate(
                session,
                repository.getEvents(),
                activity(),
                output
        );

        try (PDDocument document =
                     Loader.loadPDF(output.toFile())) {

            String text = new PDFTextStripper()
                    .getText(document);

            assertTrue(text.contains(
                    "Estimated active time: 20.0 seconds"
            ));

            assertTrue(text.contains(
                    "Estimated idle time: 10.0 seconds"
            ));
        }
    }

    // Test 5: Long timelines must create extra pages.
    @Test
    void testLongTimelineCreatesMultiplePages()
            throws IOException {

        for (int i = 0; i < 100; i++) {

            repository.addEvent(factory.createEvent(
                    "CLASS_CREATED",
                    "Class Example" + i + " created",
                    Map.of("className", "Example" + i)
            ));
        }

        Path output = tempDir.resolve("long-report.pdf");

        generator.generate(
                session,
                repository.getEvents(),
                activity(),
                output
        );

        try (PDDocument document =
                     Loader.loadPDF(output.toFile())) {

            assertTrue(document.getNumberOfPages() > 1);

            String text = new PDFTextStripper()
                    .getText(document);

            assertTrue(text.contains("Example99"));
        }
    }
}
