package io.fluxzero.home.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/** Stable identity of a space; its prefix prevents collisions with other kinds of model. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public final class SpaceId extends PlaceId<Space> {
    @JsonCreator public SpaceId(String value) { super(value, "space:"); }
}
