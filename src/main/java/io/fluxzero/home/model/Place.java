package io.fluxzero.home.model;

/** A home or space that can contain other spaces. */
public sealed interface Place permits Home, Space {
    PlaceId<?> id();
}
