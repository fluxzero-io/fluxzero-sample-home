package io.fluxzero.home.automation.api;

import io.fluxzero.home.automation.api.model.Routine;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Remove a routine and cancel its remaining scheduled work. */
public record RemoveRoutine(RoutineId routineId) {
    @Apply Routine apply(Routine routine) { return null; }
}
