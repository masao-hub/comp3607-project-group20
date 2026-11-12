
package com.comp3607.telemetry.extension;

import bluej.extensions2.BlueJ;
import bluej.extensions2.BPackage;
import bluej.extensions2.Extension;
import bluej.extensions2.MenuGenerator;

import com.comp3607.telemetry.exporters.JsonExporter;
import com.comp3607.telemetry.listeners.EditorActivityMonitor;
import com.comp3607.telemetry.nalytics.CodingActivityTracker;
import com.comp3607.telemetry.factory.TelemetryEventFactory;
import com.comp3607.telemetry.listeners.BlueJEventListener;
import com.comp3607.telemetry.repository.TelemetryRepository;
import com.comp3607.telemetry.session.SessionManager;
import com.comp3607.telemetry.reports.PdfReportGenerator;
import com.comp3607.telemetry.listeners.LifecycleMonitor;
import com.comp3607.telemetry.exporters.ExportStrategy;
import com.comp3607.telemetry.exporters.JsonExportStrategy;
import com.comp3607.telemetry.exporters.PdfExportStrategy;

import javafx.scene.control.MenuItem;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

// Main entry point for the BJTrace BlueJ extension.
public class TelemetryExtension extends Extension {

    private BlueJ bluej;

    private TelemetryRepository repository;
    private TelemetryEventFactory factory;
    private SessionManager sessionManager;
    private BlueJEventListener listener;
    private LifecycleMonitor lifecycleMonitor;
    private ExportStrategy jsonStrategy;
    private ExportStrategy pdfStrategy;

    // New component for JSON exporting.
    private JsonExporter jsonExporter;
    private CodingActivityTracker activityTracker;
    private EditorActivityMonitor editorMonitor;
    private PdfReportGenerator pdfReportGenerator;

    // Folder where our exported JSON files are stored.
    private Path exportDirectory;

    @Override
    public boolean isCompatible() {
        return getExtensionsAPIVersionMajor() >= 3;
    }

    @Override
    public String getName() {
        return "BJTrace";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Records and exports student programming telemetry.";
    }

    // BlueJ calls this method when loading BJTrace.
    @Override
    public void startup(BlueJ bluej) {

        this.bluej = bluej;

        // Create core telemetry components.
        repository = new TelemetryRepository();
        factory = new TelemetryEventFactory();

        sessionManager = new SessionManager(
                "STUDENT_001",
                repository
        );

        // Create the JSON exporter.
        jsonExporter = new JsonExporter();
        pdfReportGenerator = new PdfReportGenerator();
        jsonStrategy = new JsonExportStrategy(jsonExporter);
        pdfStrategy = new PdfExportStrategy(pdfReportGenerator);

        // Use BlueJ's own user configuration directory.
        exportDirectory = bluej.getUserConfigDir()
                .toPath()
                .resolve("telemetry-exports");

        // Start recording a session.
        sessionManager.startSession();

        // Create and register the event listener.
        listener = new BlueJEventListener(
                repository,
                factory
        );

        bluej.addPackageListener(listener);
        bluej.addClassListener(listener);
        bluej.addCompileListener(listener);
        bluej.addInvocationFinishedListener(listener);

        // Create the activity tracker.
        // Ten seconds without a detected edit means idle.
        activityTracker = new CodingActivityTracker(
                System.currentTimeMillis(),
                10_000
        );

        // Connect the tracker to BlueJ's editor monitor.
        editorMonitor = new EditorActivityMonitor(
                bluej,
                repository,
                factory,
                activityTracker
        );

        // Start checking Java editors every two seconds.
        editorMonitor.start();
        // Monitor project, class and compiled-method changes.
        lifecycleMonitor = new LifecycleMonitor(
                bluej,
                repository,
                factory
        );

        lifecycleMonitor.start();


        // Add an export command to BlueJ's Tools menu.
        bluej.setMenuGenerator(new MenuGenerator() {


            @Override
            public MenuItem getToolsMenuItem(BPackage bPackage) {

                // BlueJ's API allows one returned menu item here.
                // Use a submenu to provide both export formats.
                javafx.scene.control.Menu menu =
                        new javafx.scene.control.Menu("BJTrace");

                MenuItem jsonItem =
                        new MenuItem("Export JSON");

                jsonItem.setOnAction(action -> exportJson());

                MenuItem pdfItem =
                        new MenuItem("Export PDF");

                pdfItem.setOnAction(action -> exportPdf());

                menu.getItems().addAll(jsonItem, pdfItem);

                return menu;
            }
        });

        System.out.println(
                "[BlueJ Telemetry] BJTrace started."
        );

        System.out.println(
                "[BlueJ Telemetry] JSON exports: "
                        + exportDirectory
        );
    }

    // Generates a unique JSON filename.
    private Path createExportPath() {

        String timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern(
                        "yyyyMMdd-HHmmss"
                )
        );

        // Unique suffix prevents accidental file replacement.
        String uniqueSuffix = UUID.randomUUID()
                .toString()
                .substring(0, 8);

        String filename = "telemetry-"
                + timestamp
                + "-"
                + uniqueSuffix
                + ".json";

        return exportDirectory.resolve(filename);
    }

    // Exports the current session into a JSON file.
    private void exportJson() {

        if (jsonExporter == null
                || sessionManager == null
                || repository == null
                || activityTracker == null) {
            return;
        }

        try {

            Path destination = createExportPath();

            Path savedFile = jsonStrategy.export(
                    sessionManager,
                    repository,
                    activityTracker,
                    destination
            );

            System.out.println(
                    "[BlueJ Telemetry] JSON saved: "
                            + savedFile
            );

            System.out.println(
                    "[BlueJ Telemetry] Exported events: "
                            + repository.getEventCount()
            );

        } catch (IOException exception) {

            System.err.println(
                    "[BlueJ Telemetry] JSON export failed: "
                            + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }


    // Generates a PDF report from the current session.
    private void exportPdf() {

        if (pdfReportGenerator == null
                || sessionManager == null
                || repository == null
                || activityTracker == null) {
            return;
        }

        try {

            // Generate a unique filename.
            Path destination = createExportPath();

            // Change the extension from .json to .pdf.
            String filename = destination.getFileName()
                    .toString()
                    .replaceFirst("\\.json$", ".pdf");

            Path pdfPath = destination
                    .getParent()
                    .resolve(filename);

            // Calculate the latest activity statistics.
            CodingActivityTracker.ActivitySummary summary =
                    activityTracker.snapshot(
                            System.currentTimeMillis()
                    );

            // Create the report from the recorded events.
            Path savedFile = pdfStrategy.export(
                    sessionManager,
                    repository,
                    activityTracker,
                    pdfPath
            );

            System.out.println(
                    "[BlueJ Telemetry] PDF saved: "
                            + savedFile
            );

        } catch (IOException | RuntimeException exception) {

            System.err.println(
                    "[BlueJ Telemetry] PDF export failed: "
                            + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }

    // BlueJ calls this when unloading the extension.
    @Override
    public void terminate() {
        // Stop source monitoring before exporting.
        if (editorMonitor != null) {
            editorMonitor.stop();
            if (lifecycleMonitor != null) {
                lifecycleMonitor.stop();
            }
        }

        // Stop receiving new BlueJ notifications.
        if (bluej != null) {

            if (listener != null) {
                bluej.removePackageListener(listener);
                bluej.removeClassListener(listener);
                bluej.removeCompileListener(listener);
                bluej.removeInvocationFinishedListener(listener);
            }

            // Remove our menu generator.
            bluej.setMenuGenerator(null);
        }

        // End the session before the final export.
        if (sessionManager != null
                && sessionManager.isActive()) {

            sessionManager.endSession();
        }

        // Automatically save a final JSON file.
        exportJson();
        exportPdf();

        if (repository != null) {
            System.out.println(
                    "[BlueJ Telemetry] Total events: "
                            + repository.getEventCount()
            );
        }

        System.out.println(
                "[BlueJ Telemetry] BJTrace terminated."
        );
    }
}
