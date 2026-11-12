
package com.comp3607.telemetry.reports;

import com.comp3607.telemetry.events.TelemetryEvent;
import com.comp3607.telemetry.nalytics.CodingActivityTracker;
import com.comp3607.telemetry.nalytics.ReportAggregator;
import com.comp3607.telemetry.session.SessionManager;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// Generates a PDF report from aggregated telemetry.
public class PdfReportGenerator {

    private static final float PAGE_WIDTH = 595;
    private static final float PAGE_HEIGHT = 842;

    private static final float MARGIN = 48;
    private static final float CONTENT_WIDTH =
            PAGE_WIDTH - 2 * MARGIN;

    private static final Color NAVY =
            new Color(23, 54, 93);

    private static final Color GREEN =
            new Color(36, 157, 113);

    private static final Color LIGHT_GRAY =
            new Color(221, 228, 235);

    private final PDFont normalFont = new PDType1Font(
            Standard14Fonts.FontName.HELVETICA
    );

    private final PDFont boldFont = new PDType1Font(
            Standard14Fonts.FontName.HELVETICA_BOLD
    );

    // Controls where the next line will appear.
    private static class PageWriter implements AutoCloseable {

        private final PDDocument document;
        private final PDFont normalFont;
        private final PDFont boldFont;

        private PDPageContentStream stream;
        private float y;
        private int pageNumber;

        PageWriter(
                PDDocument document,
                PDFont normalFont,
                PDFont boldFont) throws IOException {

            this.document = document;
            this.normalFont = normalFont;
            this.boldFont = boldFont;

            newPage();
        }

        private void newPage() throws IOException {

            if (stream != null) {
                stream.close();
            }

            PDPage page = new PDPage(
                    new PDRectangle(PAGE_WIDTH, PAGE_HEIGHT)
            );

            document.addPage(page);

            stream = new PDPageContentStream(
                    document,
                    page
            );

            pageNumber++;
            y = PAGE_HEIGHT - MARGIN;

            // Page number in the top-right corner.
            writeAt(
                    "BJTrace | Page " + pageNumber,
                    PAGE_WIDTH - 130,
                    PAGE_HEIGHT - 30,
                    9,
                    normalFont,
                    Color.DARK_GRAY
            );
        }

        private void ensureSpace(float required)
                throws IOException {

            if (y - required < MARGIN) {
                newPage();
            }
        }

        // Draws one line at a specific position.
        private void writeAt(
                String text,
                float x,
                float baseline,
                float fontSize,
                PDFont font,
                Color color) throws IOException {

            stream.beginText();
            stream.setFont(font, fontSize);
            stream.setNonStrokingColor(color);
            stream.newLineAtOffset(x, baseline);
            stream.showText(safeText(text));
            stream.endText();
        }

        // Draws a line and moves down the page.
        void line(
                String text,
                float fontSize,
                boolean bold,
                Color color) throws IOException {

            PDFont font = bold ? boldFont : normalFont;

            float lineHeight = fontSize + 6;

            String remaining = safeText(text);

            if (remaining.isEmpty()) {
                ensureSpace(lineHeight);
                y -= lineHeight;
                return;
            }

            // Wrap long text to fit the page width.
            while (!remaining.isEmpty()) {

                ensureSpace(lineHeight);

                int cut = remaining.length();

                while (cut > 1 &&
                        font.getStringWidth(
                                remaining.substring(0, cut)
                        ) / 1000f * fontSize > CONTENT_WIDTH) {

                    cut--;
                }

                // Prefer wrapping at a space.
                if (cut < remaining.length()) {
                    int space = remaining.lastIndexOf(' ', cut);

                    if (space > 0) {
                        cut = space;
                    }
                }

                String part = remaining.substring(0, cut).trim();

                writeAt(
                        part,
                        MARGIN,
                        y,
                        fontSize,
                        font,
                        color
                );

                y -= lineHeight;

                remaining = remaining.substring(cut).trim();
            }
        }

        void heading(String heading) throws IOException {
            ensureSpace(35);
            y -= 10;
            line(heading, 14, true, NAVY);
            y -= 5;
        }

        // Draws a horizontal bar showing one percentage.
        void bar(
                String label,
                double fraction,
                Color color) throws IOException {

            ensureSpace(55);

            line(label, 10, false, Color.DARK_GRAY);

            float barWidth = CONTENT_WIDTH;
            float barHeight = 13;

            double clamped = Math.max(
                    0.0,
                    Math.min(1.0, fraction)
            );

            stream.setNonStrokingColor(LIGHT_GRAY);
            stream.addRect(
                    MARGIN,
                    y - barHeight,
                    barWidth,
                    barHeight
            );
            stream.fill();

            stream.setNonStrokingColor(color);
            stream.addRect(
                    MARGIN,
                    y - barHeight,
                    (float) (barWidth * clamped),
                    barHeight
            );
            stream.fill();

            y -= barHeight + 17;
        }

        @Override
        public void close() throws IOException {

            if (stream != null) {
                stream.close();
                stream = null;
            }
        }
    }

    // Standard PDF fonts cannot display every Unicode symbol.
    private static String safeText(String value) {

        if (value == null) {
            return "Not available";
        }

        StringBuilder result = new StringBuilder();

        for (char character : value.toCharArray()) {

            if (character >= 32 && character <= 126) {
                result.append(character);
            } else if (character == '\n'
                    || character == '\r'
                    || character == '\t') {
                result.append(' ');
            } else {
                result.append('?');
            }
        }

        return result.toString();
    }

    private static String formatNumber(double value) {
        return String.format(java.util.Locale.US, "%.1f", value);
    }

    // Creates and saves a complete report.
    public Path generate(
            SessionManager session,
            List<TelemetryEvent> events,
            CodingActivityTracker.ActivitySummary activity,
            Path outputFile) throws IOException {

        Objects.requireNonNull(session);
        Objects.requireNonNull(events);
        Objects.requireNonNull(activity);
        Objects.requireNonNull(outputFile);

        ReportAggregator aggregator = new ReportAggregator();

        ReportAggregator.ReportData data =
                aggregator.aggregate(events);

        Path destination = outputFile.toAbsolutePath();

        if (destination.getParent() != null) {
            Files.createDirectories(destination.getParent());
        }

        try (PDDocument document = new PDDocument()) {

            try (PageWriter writer = new PageWriter(
                    document,
                    normalFont,
                    boldFont)) {

                // Report title
                writer.line(
                        "BJTRACE - STUDENT TELEMETRY REPORT",
                        18,
                        true,
                        NAVY
                );

                writer.line(
                        "COMP3607 Object Oriented Programming II",
                        10,
                        false,
                        Color.DARK_GRAY
                );

                writer.line("", 10, false, Color.BLACK);

                // Section 1: Session overview
                writer.heading("1. SESSION OVERVIEW");

                writer.line(
                        "Student ID: " + session.getStudentId(),
                        11, false, Color.BLACK
                );

                writer.line(
                        "Start: " + session.getStartTime(),
                        10, false, Color.BLACK
                );

                writer.line(
                        "End: " + session.getEndTime(),
                        10, false, Color.BLACK
                );

                writer.line(
                        "Session duration: "
                                + session.getSessionDurationSeconds()
                                + " seconds",
                        10, false, Color.BLACK
                );

                writer.line(
                        "Recorded events: " + data.totalEvents(),
                        10, false, Color.BLACK
                );

                // Section 2: Compilation statistics
                writer.heading("2. COMPILATION SUMMARY");

                writer.line(
                        "Compilation attempts: "
                                + data.compilationAttempts(),
                        11, false, Color.BLACK
                );

                writer.line(
                        "Successful: "
                                + data.successfulCompilations(),
                        11, false, Color.BLACK
                );

                writer.line(
                        "Failed: " + data.failedCompilations(),
                        11, false, Color.BLACK
                );

                String rateText =
                        data.compilationAttempts() == 0
                                ? "N/A (no attempts)"
                                : formatNumber(
                                data.compilationSuccessRate()
                        ) + "%";

                writer.line(
                        "Success rate: " + rateText,
                        11, true, NAVY
                );

                writer.bar(
                        "Compilation success",
                        data.compilationSuccessRate() / 100.0,
                        GREEN
                );

                // Section 3: Coding activity
                writer.heading("3. ESTIMATED CODING ACTIVITY");

                writer.line(
                        "Detected editing changes: "
                                + activity.detectedEdits(),
                        10, false, Color.BLACK
                );

                writer.line(
                        "Observed period: "
                                + formatNumber(activity.observedSeconds())
                                + " seconds",
                        10, false, Color.BLACK
                );

                writer.line(
                        "Estimated active time: "
                                + formatNumber(activity.activeSeconds())
                                + " seconds",
                        10, false, Color.BLACK
                );

                writer.line(
                        "Estimated idle time: "
                                + formatNumber(activity.idleSeconds())
                                + " seconds",
                        10, false, Color.BLACK
                );

                writer.line(
                        "Detected changes per minute: "
                                + formatNumber(activity.editsPerMinute()),
                        10, false, Color.BLACK
                );

                writer.line(
                        "Average gap between detected edits: "
                                + formatNumber(
                                activity.averageObservedEditIntervalSeconds()
                        ) + " seconds",
                        10, false, Color.BLACK
                );

                double totalActivity =
                        activity.activeSeconds()
                                + activity.idleSeconds();

                double activeFraction = totalActivity > 0
                        ? activity.activeSeconds() / totalActivity
                        : 0.0;

                double idleFraction = totalActivity > 0
                        ? activity.idleSeconds() / totalActivity
                        : 0.0;

                writer.bar(
                        "Estimated active time",
                        activeFraction,
                        GREEN
                );

                writer.bar(
                        "Estimated idle time",
                        idleFraction,
                        Color.GRAY
                );

                writer.line(
                        "Method: periodic source snapshots; "
                                + "not individual keystroke tracking.",
                        9, false, Color.DARK_GRAY
                );

                // Section 4: Compilation errors
                writer.heading("4. COMPILATION ERROR FREQUENCY");

                Map<String, Integer> errors =
                        data.errorFrequencies();

                if (errors.isEmpty()) {

                    writer.line(
                            "No compiler diagnostic messages recorded.",
                            10, false, Color.BLACK
                    );

                } else {

                    for (Map.Entry<String, Integer> entry
                            : errors.entrySet()) {

                        writer.line(
                                entry.getValue() + " occurrence(s): "
                                        + entry.getKey(),
                                10, false, Color.BLACK
                        );
                    }
                }


// Additional execution and runtime exception summary.
                writer.heading("RUNTIME EXECUTION SUMMARY");

                long invocations = events.stream()
                        .filter(event ->
                                "INVOCATION_FINISHED".equals(
                                        event.getEventType()))
                        .count();

                long exceptions = events.stream()
                        .filter(event ->
                                "RUNTIME_EXCEPTION_DETECTED".equals(
                                        event.getEventType()))
                        .count();

                long terminated = events.stream()
                        .filter(event ->
                                "EXECUTION_TERMINATED".equals(
                                        event.getEventType()))
                        .count();

                writer.line(
                        "Completed invocations observed: " + invocations,
                        10, false, Color.BLACK
                );

                writer.line(
                        "Exception terminations observed: " + exceptions,
                        10, false, Color.BLACK
                );

                writer.line(
                        "Forced terminations observed: " + terminated,
                        10, false, Color.BLACK
                );

                // Section 5: Event timeline
                writer.heading("5. PROGRAMMING ACTIVITY TIMELINE");

                DateTimeFormatter timeFormat =
                        DateTimeFormatter.ofPattern(
                                "yyyy-MM-dd HH:mm:ss"
                        );

                if (data.timeline().isEmpty()) {

                    writer.line(
                            "No supported timeline events were recorded.",
                            10, false, Color.BLACK
                    );

                } else {

                    for (TelemetryEvent event : data.timeline()) {

                        String timestamp =
                                event.getTimestamp()
                                        .format(timeFormat);

                        writer.line(
                                timestamp + " | "
                                        + event.getEventType(),
                                9, true, NAVY
                        );

                        writer.line(
                                event.getDescription(),
                                9, false, Color.DARK_GRAY
                        );
                    }
                }

                writer.heading("6. DATA LIMITATIONS");

                writer.line(
                        "Coding activity is estimated from periodic "
                                + "editor text comparisons.",
                        9, false, Color.DARK_GRAY
                );

                writer.line(
                        "Class creation and method definition entries "
                                + "appear only when recorded.",
                        9, false, Color.DARK_GRAY
                );

                writer.line(
                        "Compilation statistics depend on the "
                                + "events collected during the session.",
                        9, false, Color.DARK_GRAY
                );
            }

            // Save only after all content streams are closed.
            document.save(destination.toFile());
        }

        return destination;
    }
}
