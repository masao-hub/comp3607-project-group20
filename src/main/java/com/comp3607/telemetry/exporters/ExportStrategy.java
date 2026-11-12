
package com.comp3607.telemetry.exporters;

import com.comp3607.telemetry.repository.TelemetryRepository;
import com.comp3607.telemetry.session.SessionManager;
import com.comp3607.telemetry.nalytics.CodingActivityTracker;

import java.io.IOException;
import java.nio.file.Path;

// Common contract for exporting telemetry reports.
public interface ExportStrategy {

    Path export(
            SessionManager session,
            TelemetryRepository repository,
            CodingActivityTracker tracker,
            Path outputFile
    ) throws IOException;
}
