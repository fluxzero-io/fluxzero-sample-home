package io.fluxzero.home.model;

import io.fluxzero.sdk.modeling.Id;

/** Stable identity of a resident; its prefix prevents collisions with other kinds of model. */
public final class ResidentId extends Id<Resident> {
    public ResidentId(String value) { super(value, "resident:"); }
}
