package io.fluxzero.home.model;

import io.fluxzero.sdk.modeling.Id;

/** Stable identity of a home; its prefix prevents collisions with other kinds of model. */
public final class HomeId extends Id<Home> {
    public HomeId(String value) { super(value, "home:"); }
}
