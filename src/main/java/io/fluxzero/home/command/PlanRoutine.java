package io.fluxzero.home.command;

import io.fluxzero.common.serialization.Revision;
import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.home.model.Routine;
import io.fluxzero.home.model.RoutineDetails;
import io.fluxzero.home.model.RoutineId;
import io.fluxzero.home.model.RoutineTiming;
import io.fluxzero.home.model.Scene;
import io.fluxzero.home.model.SceneId;
import io.fluxzero.home.model.ScenePlan;
import io.fluxzero.sdk.common.Message;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import static io.fluxzero.home.model.Rules.require;

/** Plan a scene once or on selected days; revising a routine replaces its next scheduled execution. */
@Revision(1)
public record PlanRoutine(RoutineId routineId, HomeId homeId, @NotNull @Valid RoutineDetails details, SceneId sceneId, RoutineTiming timing) {
    @AssertLegal void validate(Graph<Home> home, @Nullable Routine routine, Message message) {
        require(timing != null, "Choose when the routine should run.");
        require(routine == null || routine.homeId().equals(homeId), "A routine cannot move between homes.");
        ScenePlan.find(home, sceneId, Scene.class);
        require(timing.nextAfter(message.getTimestamp(), home.get().timeZone()) != null, "Choose a future moment.");
    }
    @Apply Routine apply(@Nullable Routine routine, Home home, Message message) {
        return new Routine(routineId, homeId, details, sceneId, timing, true,
                timing.nextAfter(message.getTimestamp(), home.timeZone()), routine == null ? 1 : routine.generation() + 1,
                routine == null ? 0 : routine.executionCount(), routine == null ? null : routine.lastExecutedAt(), null);
    }
}
