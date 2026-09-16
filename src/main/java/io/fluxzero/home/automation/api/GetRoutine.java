package io.fluxzero.home.automation.api;

import io.fluxzero.home.automation.api.model.Routine;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.tracking.handling.HandleQuery;
import io.fluxzero.sdk.tracking.handling.Request;

/** Inspect a routine’s next execution, pause state and execution history. */
public record GetRoutine(RoutineId routineId) implements Request<Routine> {
    @HandleQuery Routine handle() { return Fluxzero.loadModel(routineId).get(); }
}
