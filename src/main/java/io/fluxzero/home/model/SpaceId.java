package io.fluxzero.home.model;

import io.fluxzero.sdk.modeling.Id;

/** Stable identity of a space; its prefix prevents collisions with other kinds of model. */
public final class SpaceId extends Id<Space> {
    public SpaceId(String value) { super(value, "space:"); }
}
