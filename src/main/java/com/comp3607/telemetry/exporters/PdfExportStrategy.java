
package com.comp3607.telemetry.exporters;

import com.comp3607.telemetry.repository.TelemetryRepository;
import com.comp3607.telemetry.session.SessionManager;
import com.comp3607.telemetry.nalytics.CodingActivityTracker;
import com.comp3607.telemetry.reports.PdfReportGenerator;

import java.io.IOException;
import java.nio.file.Path;

// PDF implementation of the export strategy.
public class PdfExportStrategy implements ExportStrategy {

    private final PdfReportGenerator generator;

    public PdfExportStrategy(PdfReportGenerator generator) {
        this.generator = generator;
    }

    @Override
    public Path export(
            SessionManager session,
            TelemetryRepository repository,
            CodingActivityTracker tracker,
            Path outputFile) throws IOException {

        return generator.generate(
                session,
                repository.getEvents(),
                tracker.snapshot(System.currentTimeMillis()),
                outputFile
        );
    }
}
