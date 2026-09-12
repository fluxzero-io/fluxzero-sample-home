package io.fluxzero.home.model;

import io.fluxzero.sdk.modeling.Id;

/** Stable identity of a zone; its prefix prevents collisions with other kinds of model. */
public final class ZoneId extends Id<Zone> {
    public ZoneId(String value) { super(value, "zone:"); }
}
