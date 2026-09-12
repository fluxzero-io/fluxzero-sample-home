package io.fluxzero.home.command;

import io.fluxzero.home.model.Routine;
import io.fluxzero.home.model.RoutineId;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Remove a routine and cancel its remaining scheduled work. */
public record RemoveRoutine(RoutineId routineId) {
    @Apply Routine apply(Routine routine) { return null; }
}
