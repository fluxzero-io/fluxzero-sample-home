package io.fluxzero.home.automation;

import io.fluxzero.home.command.ActivateScene;
import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.Routine;
import io.fluxzero.home.model.RoutineId;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.common.Message;
import io.fluxzero.sdk.common.exception.FunctionalException;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.modeling.Parent;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.persisting.eventsourcing.InterceptApply;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** A planned execution belongs to its routine; obsolete or early deliveries do not change the home. */
public record RunRoutine(@Parent RoutineId routineId, long generation, Instant due) {
    @InterceptApply Object prepare(@Nullable Routine routine, Graph<Home> home, Message message) {
        if (routine == null || !routine.enabled() || routine.generation() != generation
                || !Objects.equals(routine.nextRun(), due) || due.isAfter(message.getTimestamp())) return null;
        try {
            Fluxzero.assertLegal(new ActivateScene(routine.sceneId()));
            return List.of(new ActivateScene(routine.sceneId()), this);
        } catch (FunctionalException failure) {
            return new PauseFailedRoutine(routineId, generation, due, failure.getMessage());
        }
    }
    @Apply Routine apply(Routine routine, Home home, Message message) {
        var next = routine.timing().nextAfter(message.getTimestamp(), home.timeZone());
        return routine.withNextRun(next).withEnabled(next != null).withGeneration(generation + 1)
                .withExecutionCount(routine.executionCount() + 1).withLastExecutedAt(message.getTimestamp());
    }
}
