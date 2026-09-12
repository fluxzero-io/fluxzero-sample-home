package io.fluxzero.home.query;

import io.fluxzero.home.model.Routine;
import io.fluxzero.home.model.RoutineId;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.tracking.handling.HandleQuery;
import io.fluxzero.sdk.tracking.handling.Request;

/** Inspect a routine’s next execution, pause state and execution history. */
public record GetRoutine(RoutineId routineId) implements Request<Routine> {
    @HandleQuery Routine handle() { return Fluxzero.loadModel(routineId).get(); }
}
