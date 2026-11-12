
package com.comp3607.telemetry.listeners;

import bluej.extensions2.BlueJ;
import bluej.extensions2.BProject;
import bluej.extensions2.BPackage;
import bluej.extensions2.BClass;
import bluej.extensions2.BMethod;

import com.comp3607.telemetry.factory.TelemetryEventFactory;
import com.comp3607.telemetry.repository.TelemetryRepository;
import com.comp3607.telemetry.nalytics.LifecycleChangeDetector;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

// Detects changes to BlueJ's open projects and classes.
public class LifecycleMonitor {

    private final BlueJ bluej;
    private final TelemetryRepository repository;
    private final TelemetryEventFactory factory;

    private final LifecycleChangeDetector detector =
            new LifecycleChangeDetector();

    // The last successful snapshot for each project.
    private final Map<String, Set<String>> knownClasses =
            new HashMap<>();

    // Known compiled method signatures for each class.
    private final Map<String, Set<String>> knownMethods =
            new HashMap<>();

    private Timeline timeline;

    public LifecycleMonitor(
            BlueJ bluej,
            TelemetryRepository repository,
            TelemetryEventFactory factory) {

        this.bluej = Objects.requireNonNull(bluej);
        this.repository = Objects.requireNonNull(repository);
        this.factory = Objects.requireNonNull(factory);
    }

    // Starts checking every five seconds.
    public void start() {

        if (timeline != null) {
            return;
        }

        // Establish an initial baseline.
        scan();

        timeline = new Timeline(
                new KeyFrame(
                        Duration.seconds(5),
                        event -> scan()
                )
        );

        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    public void stop() {

        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }

        knownClasses.clear();
        knownMethods.clear();
    }

    private void record(
            String type,
            String description,
            Map<String, String> details) {

        repository.addEvent(
                factory.createEvent(
                        type,
                        description,
                        details
                )
        );
    }

    // Inspects projects, classes and compiled methods.
    private void scan() {

        Set<String> openProjectIds = new HashSet<>();

        try {

            BProject[] projects = bluej.getOpenProjects();

            if (projects == null) {
                return;
            }

            for (BProject project : projects) {

                try {

                    String projectId =
                            project.getDir().getAbsolutePath();

                    String projectName = project.getName();

                    openProjectIds.add(projectId);

                    boolean previouslyKnown =
                            knownClasses.containsKey(projectId);

                    Set<String> currentClasses =
                            new HashSet<>();

                    Set<String> currentMethods =
                            new HashSet<>();

                    BPackage[] packages = project.getPackages();

                    for (BPackage bPackage : packages) {

                        BClass[] classes = bPackage.getClasses();

                        for (BClass bClass : classes) {

                            try {

                                String className = bClass.getName();

                                String classId =
                                        projectId + "::" + className;

                                currentClasses.add(classId);

                                // Only compiled classes have reliable
                                // declared-method information here.
                                if (bClass.isCompiled()) {

                                    BMethod[] methods =
                                            bClass.getDeclaredMethods();

                                    if (methods != null) {

                                        for (BMethod method : methods) {

                                            String signature =
                                                    method.toString();

                                            currentMethods.add(
                                                    classId + "::" + signature
                                            );
                                        }
                                    }
                                }

                            } catch (Exception exception) {

                                System.err.println(
                                        "[BJTrace] Skipped class: "
                                                + exception.getMessage()
                                );
                            }
                        }
                    }

                    if (previouslyKnown) {

                        LifecycleChangeDetector.Changes changes =
                                detector.compare(
                                        knownClasses.get(projectId),
                                        currentClasses
                                );

                        for (String added : changes.added()) {

                            record(
                                    "CLASS_CREATED_DETECTED",
                                    "New class detected",
                                    Map.of(
                                            "project", projectName,
                                            "classId", added,
                                            "detectionMethod",
                                            "SNAPSHOT_COMPARISON"
                                    )
                            );
                        }

                        // Class deletion is also captured by
                        // the existing BlueJ ClassListener.
                        // We avoid recording it twice here.

                        Set<String> previousMethods =
                                knownMethods.getOrDefault(
                                        projectId,
                                        Set.of()
                                );

                        LifecycleChangeDetector.Changes methodChanges =
                                detector.compare(
                                        previousMethods,
                                        currentMethods
                                );

                        for (String addedMethod
                                : methodChanges.added()) {

                            record(
                                    "METHOD_OBSERVED",
                                    "New compiled method observed",
                                    Map.of(
                                            "project", projectName,
                                            "methodId", addedMethod,
                                            "detectionMethod",
                                            "COMPILED_METHOD_COMPARISON"
                                    )
                            );
                        }

                    } else if (!knownClasses.isEmpty()) {

                        // A project has appeared after monitoring began.
                        record(
                                "PROJECT_OPENED_DETECTED",
                                "BlueJ project opened",
                                Map.of(
                                        "project", projectName,
                                        "projectPath", projectId
                                )
                        );
                    }

                    knownClasses.put(projectId, currentClasses);
                    knownMethods.put(projectId, currentMethods);

                } catch (Exception exception) {

                    System.err.println(
                            "[BJTrace] Cannot inspect project: "
                                    + exception.getMessage()
                    );
                }
            }

            // Record projects that disappeared from the open list.
            Set<String> disappeared =
                    new HashSet<>(knownClasses.keySet());

            disappeared.removeAll(openProjectIds);

            for (String projectId : disappeared) {

                record(
                        "PROJECT_CLOSED_DETECTED",
                        "BlueJ project closed",
                        Map.of("projectPath", projectId)
                );

                knownClasses.remove(projectId);
                knownMethods.remove(projectId);
            }

        } catch (Exception exception) {

            System.err.println(
                    "[BJTrace] Lifecycle scan failed: "
                            + exception.getMessage()
            );
        }
    }
}
