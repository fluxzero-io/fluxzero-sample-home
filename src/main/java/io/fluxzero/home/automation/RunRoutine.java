package io.fluxzero.home.automation;

import io.fluxzero.home.command.ActivateScene;
import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.Routine;
import io.fluxzero.home.model.RoutineId;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.common.Message;
import io.fluxzero.sdk.common.exception.FunctionalException;
import io.fluxzero.sdk.modeling.Parent;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.persisting.eventsourcing.InterceptApply;
import io.fluxzero.sdk.tracking.handling.HandleCommand;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** A planned execution belongs to its routine; obsolete or early deliveries do not change the home. */
public record RunRoutine(@Parent RoutineId routineId, long generation, Instant due) {
    @HandleCommand
    void execute() {
        try {
            Fluxzero.assertAndApply(this);
        } catch (FunctionalException failure) {
            Fluxzero.assertAndApply(new PauseFailedRoutine(routineId, generation, due, failure.getMessage()));
        }
    }

    @InterceptApply Object prepare(@Nullable Routine routine, Message message) {
        if (routine == null || !routine.enabled() || routine.generation() != generation
                || !Objects.equals(routine.nextRun(), due) || due.isAfter(message.getTimestamp())) return null;
        return List.of(new ActivateScene(routine.sceneId()), this);
    }
    @Apply Routine apply(Routine routine, Home home, Message message) {
        var next = routine.timing().nextAfter(message.getTimestamp(), home.timeZone());
        return routine.withNextRun(next).withEnabled(next != null).withGeneration(generation + 1);
    }
}
