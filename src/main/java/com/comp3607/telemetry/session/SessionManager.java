
package com.comp3607.telemetry.session;

import com.comp3607.telemetry.repository.TelemetryRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

// Tracks a student's programming session.
public class SessionManager {

    // Placeholder identifier for the student.
    private final String studentId;

    // Repository containing events for this session.
    private final TelemetryRepository repository;

    // The time the session started.
    private LocalDateTime startTime;

    // The time the session ended.
    private LocalDateTime endTime;

    // Indicates whether a session is currently running.
    private boolean active;

    // Constructor: prepares a new session manager.
    public SessionManager(
            String studentId,
            TelemetryRepository repository) {

        this.studentId = Objects.requireNonNull(
                studentId, "Student ID cannot be null");

        this.repository = Objects.requireNonNull(
                repository, "Repository cannot be null");

        this.active = false;
    }

    // Starts a new programming session.
    public void startSession() {

        if (active) {
            throw new IllegalStateException(
                    "A session is already active");
        }

        repository.clearEvents();

        startTime = LocalDateTime.now();
        endTime = null;
        active = true;
    }

    // Ends the current programming session.
    public void endSession() {

        if (!active) {
            throw new IllegalStateException(
                    "No active session to end");
        }

        endTime = LocalDateTime.now();
        active = false;
    }

    // Returns the total duration in seconds.
    public long getSessionDurationSeconds() {

        if (startTime == null) {
            return 0;
        }

        LocalDateTime finishTime;

        if (active) {
            finishTime = LocalDateTime.now();
        } else {
            finishTime = endTime;
        }

        return Duration.between(
                startTime, finishTime).getSeconds();
    }

    // Counts successful and failed compilation attempts.
    public int getTotalCompilationAttempts() {

        int successful =
                repository.getEventsByType(
                        "COMPILATION_SUCCESS").size();

        int failed =
                repository.getEventsByType(
                        "COMPILATION_ERROR").size();

        return successful + failed;
    }

    // Returns the student's placeholder ID.
    public String getStudentId() {
        return studentId;
    }

    // Returns the session start time.
    public LocalDateTime getStartTime() {
        return startTime;
    }

    // Returns the session end time.
    public LocalDateTime getEndTime() {
        return endTime;
    }

    // Checks whether the session is active.
    public boolean isActive() {
        return active;
    }
}
