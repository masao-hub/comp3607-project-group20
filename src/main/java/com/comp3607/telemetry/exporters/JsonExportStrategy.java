
package com.comp3607.telemetry.exporters;

import com.comp3607.telemetry.repository.TelemetryRepository;
import com.comp3607.telemetry.session.SessionManager;
import com.comp3607.telemetry.nalytics.CodingActivityTracker;

import java.io.IOException;
import java.nio.file.Path;

// JSON implementation of the export strategy.
public class JsonExportStrategy implements ExportStrategy {

    private final JsonExporter exporter;

    public JsonExportStrategy(JsonExporter exporter) {
        this.exporter = exporter;
    }

    @Override
    public Path export(
            SessionManager session,
            TelemetryRepository repository,
            CodingActivityTracker tracker,
            Path outputFile) throws IOException {

        return exporter.export(
                session,
                repository,
                tracker,
                outputFile
        );
    }
}
