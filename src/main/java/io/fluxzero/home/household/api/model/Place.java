package io.fluxzero.home.household.api.model;

import io.fluxzero.home.household.api.PlaceId;

/** A home or space that can contain other spaces. */
public sealed interface Place permits Home, Space {
    PlaceId<?> id();
}
