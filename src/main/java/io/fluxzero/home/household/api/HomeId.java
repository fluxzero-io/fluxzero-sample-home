package io.fluxzero.home.household.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.fluxzero.home.household.api.model.Home;

/** Stable identity of a home; its prefix prevents collisions with other kinds of model. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public final class HomeId extends PlaceId<Home> {
    @JsonCreator public HomeId(String value) { super(value, "home:"); }
}
