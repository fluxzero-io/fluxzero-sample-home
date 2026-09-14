package io.fluxzero.home.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/** Stable identity of a home; its prefix prevents collisions with other kinds of model. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public final class HomeId extends PlaceId<Home> {
    @JsonCreator public HomeId(String value) { super(value, "home:"); }
}
