
package com.comp3607.telemetry.listeners;

import bluej.extensions2.BlueJ;
import bluej.extensions2.BProject;
import bluej.extensions2.BPackage;
import bluej.extensions2.BClass;
import bluej.extensions2.editor.JavaEditor;
import bluej.extensions2.editor.TextLocation;
import bluej.extensions2.ProjectNotOpenException;
import bluej.extensions2.PackageNotFoundException;

import com.comp3607.telemetry.factory.TelemetryEventFactory;
import com.comp3607.telemetry.repository.TelemetryRepository;
import com.comp3607.telemetry.nalytics.CodingActivityTracker;


import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

// Monitors changes in currently open BlueJ Java editors.
public class EditorActivityMonitor {

    // How often we inspect open Java editors.
    private static final double POLL_SECONDS = 2.0;

    private final BlueJ bluej;
    private final TelemetryRepository repository;
    private final TelemetryEventFactory factory;
    private final CodingActivityTracker tracker;

    // Remembers the previous text for each open editor.
    private final Map<String, String> previousTexts =
            new HashMap<>();

    private Timeline timeline;

    public EditorActivityMonitor(
            BlueJ bluej,
            TelemetryRepository repository,
            TelemetryEventFactory factory,
            CodingActivityTracker tracker) {

        this.bluej = Objects.requireNonNull(bluej);
        this.repository = Objects.requireNonNull(repository);
        this.factory = Objects.requireNonNull(factory);
        this.tracker = Objects.requireNonNull(tracker);
    }

    // Starts periodic checks of open Java editors.
    public void start() {

        if (timeline != null) {
            return;
        }

        timeline = new Timeline(
                new KeyFrame(
                        Duration.seconds(POLL_SECONDS),
                        event -> inspectEditors()
                )
        );

        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        System.out.println(
                "[BlueJ Telemetry] Editor monitoring started."
        );
    }

    // Stops periodic editor monitoring.
    public void stop() {

        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }

        previousTexts.clear();

        System.out.println(
                "[BlueJ Telemetry] Editor monitoring stopped."
        );
    }

    // Retrieves the current text from a Java editor.
    private String readEditorText(JavaEditor editor) {

        int textLength = editor.getTextLength();

        TextLocation beginning = new TextLocation(0, 0);

        TextLocation ending =
                editor.getTextLocationFromOffset(textLength);

        return editor.getText(beginning, ending);
    }

    // Searches all open BlueJ projects for open Java editors.
    private void inspectEditors() {

        Set<String> currentlyOpenEditors = new HashSet<>();

        try {

            BProject[] projects = bluej.getOpenProjects();

            for (BProject project : projects) {

                String projectPath =
                        project.getDir().getAbsolutePath();

                BPackage[] packages = project.getPackages();

                for (BPackage bPackage : packages) {

                    BClass[] classes = bPackage.getClasses();

                    for (BClass bClass : classes) {

                        JavaEditor editor =
                                bClass.getJavaEditorIfOpen();

                        if (editor == null) {
                            continue;
                        }

                        // Only monitor visible editors.
                        if (!editor.isVisible()) {
                            continue;
                        }

                        String className = bClass.getName();

                        // Unique identity across different projects.
                        String editorId =
                                projectPath + "::" + className;

                        currentlyOpenEditors.add(editorId);

                        try {

                            String currentText =
                                    readEditorText(editor);

                            String oldText =
                                    previousTexts.put(
                                            editorId,
                                            currentText
                                    );

                            // First observation establishes a baseline.
                            if (oldText == null) {
                                continue;
                            }

                            if (!oldText.equals(currentText)) {

                                long now =
                                        System.currentTimeMillis();

                                tracker.recordEdit(now);

                                repository.addEvent(
                                        factory.createEvent(
                                                "EDITOR_CHANGE_DETECTED",
                                                "Source change detected in "
                                                        + className,
                                                Map.of(
                                                        "className", className,
                                                        "pollIntervalSeconds",
                                                        String.valueOf(
                                                                POLL_SECONDS),
                                                        "previousLength",
                                                        String.valueOf(
                                                                oldText.length()),
                                                        "currentLength",
                                                        String.valueOf(
                                                                currentText.length()),
                                                        "detectionMethod",
                                                        "SOURCE_SNAPSHOT"
                                                )
                                        )
                                );

                                System.out.println(
                                        "[BlueJ Telemetry] "
                                                + "Editor change: "
                                                + className
                                );
                            }

                        } catch (RuntimeException exception) {

                            System.err.println(
                                    "[BlueJ Telemetry] Cannot inspect "
                                            + className + ": "
                                            + exception.getMessage()
                            );

                            // Next successful reading starts a new baseline.
                            previousTexts.remove(editorId);
                        }
                    }
                }
            }


        } catch (ProjectNotOpenException
                 | PackageNotFoundException exception) {

            System.err.println(
                    "[BlueJ Telemetry] A project or package "
                            + "was closed during monitoring: "
                            + exception.getMessage()
            );

            // Keep previous snapshots and try again later.
            return;

        } catch (RuntimeException exception) {

            System.err.println(
                    "[BlueJ Telemetry] Editor scan failed: "
                            + exception.getMessage()
            );

            return;
        }


        // Forget editors that are no longer open.
        previousTexts.keySet().retainAll(currentlyOpenEditors);
    }
}
