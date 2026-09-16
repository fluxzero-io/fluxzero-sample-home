package io.fluxzero.home.automation.api;

import io.fluxzero.home.automation.api.model.Routine;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Pause future executions while retaining the routine and its history. */
public record PauseRoutine(RoutineId routineId) {
    @Apply Routine apply(Routine routine) {
        return routine.enabled() ? routine.withEnabled(false).withNextRun(null).withGeneration(routine.generation() + 1) : routine;
    }
}
