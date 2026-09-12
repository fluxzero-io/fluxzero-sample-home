package io.fluxzero.home.model;

import io.fluxzero.sdk.modeling.Id;

/** Stable identity of a routine; its prefix prevents collisions with other kinds of model. */
public final class RoutineId extends Id<Routine> {
    public RoutineId(String value) { super(value, "routine:"); }
}
