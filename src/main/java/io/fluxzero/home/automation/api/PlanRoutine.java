package io.fluxzero.home.automation.api;

import io.fluxzero.home.automation.api.model.Routine;
import io.fluxzero.home.automation.api.model.RoutineDetails;
import io.fluxzero.home.automation.api.model.RoutineTiming;
import io.fluxzero.home.household.api.HomeId;
import io.fluxzero.home.household.api.model.Home;
import io.fluxzero.home.scenes.api.SceneId;
import io.fluxzero.home.scenes.api.model.Scene;
import io.fluxzero.sdk.common.Message;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Plan a scene once or on selected days; revising a routine replaces its next scheduled execution. */
public record PlanRoutine(RoutineId routineId, HomeId homeId, @NotNull @Valid RoutineDetails details,
                          @NotNull SceneId sceneId, @NotNull @Valid RoutineTiming timing) {
    @AssertLegal void remainsInHome(@Nullable Routine routine) {
        if (routine != null && !routine.homeId().equals(homeId)) {
            throw new IllegalCommandException("A routine cannot move between homes.");
        }
    }

    @AssertLegal void sceneBelongsToHome(Graph<Home> home) {
        if (home.find(sceneId, Scene.class).isEmpty()) {
            throw new IllegalCommandException("Choose an existing scene from this home.");
        }
    }

    @AssertLegal void hasFutureOccurrence(Home home, Message message) {
        if (timing.nextAfter(message.getTimestamp(), home.timeZone()) == null) {
            throw new IllegalCommandException("Choose a future moment.");
        }
    }
    @Apply Routine apply(@Nullable Routine routine, Home home, Message message) {
        return new Routine(routineId, homeId, details, sceneId, timing, true,
                timing.nextAfter(message.getTimestamp(), home.timeZone()), routine == null ? 1 : routine.generation() + 1, null);
    }
}
