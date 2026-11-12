
package com.comp3607.telemetry.nalytics;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

// Compares two snapshots of names.
public class LifecycleChangeDetector {

    // The changes found between two snapshots.
    public record Changes(
            Set<String> added,
            Set<String> removed) {
    }

    public Changes compare(
            Set<String> previous,
            Set<String> current) {

        Objects.requireNonNull(previous);
        Objects.requireNonNull(current);

        // Find names present now but absent before.
        Set<String> added = new HashSet<>(current);
        added.removeAll(previous);

        // Find names that disappeared.
        Set<String> removed = new HashSet<>(previous);
        removed.removeAll(current);

        return new Changes(
                Set.copyOf(added),
                Set.copyOf(removed)
        );
    }
}
