package io.fluxzero.home.command;

import io.fluxzero.home.model.Routine;
import io.fluxzero.home.model.RoutineId;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Pause future executions while retaining the routine and its history. */
public record PauseRoutine(RoutineId routineId) {
    @Apply Routine apply(Routine routine) {
        return routine.enabled() ? routine.withEnabled(false).withNextRun(null).withGeneration(routine.generation() + 1) : routine;
    }
}
