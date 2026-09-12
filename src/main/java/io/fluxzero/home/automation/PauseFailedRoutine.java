package io.fluxzero.home.automation;

import io.fluxzero.home.model.Routine;
import io.fluxzero.home.model.RoutineId;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.persisting.eventsourcing.InterceptApply;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.Objects;

/** A rejected scene pauses its routine visibly; no device intentions are committed. */
public record PauseFailedRoutine(RoutineId routineId, long generation, Instant due, String problem) {
    @InterceptApply Object ignoreObsolete(@Nullable Routine routine) {
        return routine == null || !routine.enabled() || routine.generation() != generation
                || !Objects.equals(routine.nextRun(), due) ? null : this;
    }
    @Apply Routine apply(Routine routine) {
        return routine.withEnabled(false).withNextRun(null).withGeneration(generation + 1).withProblem(problem);
    }
}
