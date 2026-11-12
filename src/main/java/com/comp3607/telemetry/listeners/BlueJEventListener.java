
package com.comp3607.telemetry.listeners;

import bluej.extensions2.event.ClassEvent;
import bluej.extensions2.event.ClassListener;
import bluej.extensions2.event.CompileEvent;
import bluej.extensions2.event.CompileListener;
import bluej.extensions2.event.InvocationFinishedEvent;
import bluej.extensions2.event.InvocationFinishedListener;
import bluej.extensions2.event.PackageEvent;
import bluej.extensions2.event.PackageListener;

import com.comp3607.telemetry.events.TelemetryEvent;
import com.comp3607.telemetry.factory.TelemetryEventFactory;
import com.comp3607.telemetry.repository.TelemetryRepository;

import java.io.File;
import java.util.Map;
import java.util.Objects;

// Listens for supported student actions inside BlueJ.
public class BlueJEventListener implements
        PackageListener,
        ClassListener,
        CompileListener,
        InvocationFinishedListener {

    // Stores our collected telemetry events.
    private final TelemetryRepository repository;

    // Creates structured telemetry events.
    private final TelemetryEventFactory factory;

    // Temporarily remembers a compilation error.
    private String lastErrorMessage = "";
    private int lastErrorLine = 0;

    // Constructor: connects the listener to our repository.
    public BlueJEventListener(
            TelemetryRepository repository,
            TelemetryEventFactory factory) {

        this.repository = Objects.requireNonNull(repository);
        this.factory = Objects.requireNonNull(factory);
    }

    // Saves a telemetry event and prints a debug message.
    private void record(TelemetryEvent event) {
        repository.addEvent(event);

        System.out.println(
                "[BlueJ Telemetry] " + event
        );
    }

    // Finds the first Java filename in a compilation event.
    private String getCompiledFileName(CompileEvent event) {

        File[] files = event.getFiles();

        if (files != null && files.length > 0
                && files[0] != null) {
            return files[0].getName();
        }

        return "UnknownFile";
    }

    // Called when a BlueJ package is opened.
    @Override
    public void packageOpened(PackageEvent event) {

        record(factory.createEvent(
                "PACKAGE_OPENED",
                "A BlueJ package was opened",
                Map.of("source", "BlueJ")
        ));
    }

    // Called when a BlueJ package is closing.
    @Override
    public void packageClosing(PackageEvent event) {

        record(factory.createEvent(
                "PACKAGE_CLOSING",
                "A BlueJ package is closing",
                Map.of("source", "BlueJ")
        ));
    }

    // Called when a class's compilation state changes.

    @Override
    public void classStateChanged(ClassEvent event) {

        String className = event.getBClass().getName();

        boolean compiled = event.isClassCompiled();

        boolean hasError = !compiled && event.hasError();

        record(factory.createEvent(
                "CLASS_STATE_CHANGED",
                "Compilation state changed for " + className,
                Map.of(
                        "className", className,
                        "compiled", String.valueOf(compiled),
                        "hasError", String.valueOf(hasError)
                )
        ));
    }

    // Called when a class is renamed.
    @Override
    public void classNameChanged(ClassEvent event) {

        String newName = event.getBClass().getName();
        String oldName = event.getOldName();

        record(factory.createEvent(
                "CLASS_RENAMED",
                "Class renamed from " + oldName
                        + " to " + newName,
                Map.of(
                        "oldName", oldName,
                        "newName", newName
                )
        ));
    }

    // Called when a class is deleted.
    @Override
    public void classRemoved(ClassEvent event) {

        String className = event.getBClass().getName();

        record(factory.createEvent(
                "CLASS_REMOVED",
                "Class " + className + " was removed",
                Map.of("className", className)
        ));
    }

    // Called when a compilation starts.
    @Override
    public void compileStarted(CompileEvent event) {

        if (!event.isUserGeneratedCompilation()) {
            return;
        }

        lastErrorMessage = "";
        lastErrorLine = 0;

        String fileName = getCompiledFileName(event);

        record(factory.createEvent(
                "COMPILATION_STARTED",
                "Compilation started for " + fileName,
                Map.of("fileName", fileName)
        ));
    }

    // Called when BlueJ reports a compilation error.
    @Override
    public void compileError(CompileEvent event) {

        if (!event.isUserGeneratedCompilation()) {
            return;
        }

        String message = event.getErrorMessage();
        int line = event.getErrorLineNumber();

        lastErrorMessage =
                message == null ? "Unknown error" : message;

        lastErrorLine = Math.max(0, line);

        record(factory.createEvent(
                "COMPILATION_DIAGNOSTIC",
                "Compilation error reported",
                Map.of(
                        "fileName", getCompiledFileName(event),
                        "errorMessage", lastErrorMessage,
                        "lineNumber",
                        String.valueOf(lastErrorLine)
                )
        ));
    }

    // Called when BlueJ reports a compilation warning.
    @Override
    public void compileWarning(CompileEvent event) {

        if (!event.isUserGeneratedCompilation()) {
            return;
        }

        String message = event.getErrorMessage();

        record(factory.createEvent(
                "COMPILATION_WARNING",
                "Compiler warning reported",
                Map.of(
                        "fileName", getCompiledFileName(event),
                        "message",
                        message == null ? "Unknown warning" : message
                )
        ));
    }

    // Called when compilation succeeds.
    @Override
    public void compileSucceeded(CompileEvent event) {

        if (!event.isUserGeneratedCompilation()) {
            return;
        }

        record(factory.createCompilationSuccessEvent(
                getCompiledFileName(event)
        ));
    }

    // Called when compilation finishes unsuccessfully.
    @Override
    public void compileFailed(CompileEvent event) {

        if (!event.isUserGeneratedCompilation()) {
            return;
        }

        String message = lastErrorMessage.isEmpty()
                ? "Compilation failed"
                : lastErrorMessage;

        record(factory.createCompilationErrorEvent(
                getCompiledFileName(event),
                message,
                lastErrorLine
        ));

        lastErrorMessage = "";
        lastErrorLine = 0;
    }

    // Called when a method or constructor invocation finishes.

    @Override
    public void invocationFinished(
            InvocationFinishedEvent event) {

        String className = event.getClassName();
        String methodName = event.getMethodName();

        if (className == null) {
            className = "UnknownClass";
        }

        if (methodName == null) {
            methodName = "<constructor>";
        }

        String outcome = event.getEventType().name();

        // Record every completed invocation.
        record(factory.createEvent(
                "INVOCATION_FINISHED",
                "Invocation of " + methodName
                        + " in " + className,
                Map.of(
                        "className", className,
                        "methodName", methodName,
                        "outcome", outcome
                )
        ));

        // Record an additional event for exception termination.
        if (event.getEventType()
                == InvocationFinishedEvent.EventType.EXCEPTION_EXIT) {

            record(factory.createEvent(
                    "RUNTIME_EXCEPTION_DETECTED",
                    "Invocation ended with an exception",
                    Map.of(
                            "className", className,
                            "methodName", methodName,
                            "outcome", outcome
                    )
            ));
        }

        // Record forced termination separately.
        if (event.getEventType()
                == InvocationFinishedEvent.EventType.TERMINATED_EXIT) {

            record(factory.createEvent(
                    "EXECUTION_TERMINATED",
                    "Invocation was terminated",
                    Map.of(
                            "className", className,
                            "methodName", methodName,
                            "outcome", outcome
                    )
            ));
        }
    }


}
