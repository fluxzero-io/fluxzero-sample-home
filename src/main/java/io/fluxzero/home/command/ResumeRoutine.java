package io.fluxzero.home.command;

import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.Routine;
import io.fluxzero.home.model.RoutineId;
import io.fluxzero.sdk.common.Message;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;

/** Resume at the next future occurrence, without replaying missed occurrences. */
public record ResumeRoutine(RoutineId routineId) {
    @AssertLegal void validate(Routine routine, Home home, Message message) {
        if (routine.timing().nextAfter(message.getTimestamp(), home.timeZone()) == null) {
            throw new IllegalCommandException("This one-off moment has already passed.");
        }
    }
    @Apply Routine apply(Routine routine, Home home, Message message) {
        return routine.enabled() ? routine : routine.withEnabled(true).withProblem(null).withGeneration(routine.generation() + 1)
                .withNextRun(routine.timing().nextAfter(message.getTimestamp(), home.timeZone()));
    }
}
