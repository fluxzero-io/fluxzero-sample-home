package io.fluxzero.home.model;

import java.time.Instant;

/** The household mode before and after one committed change. */
public record HomeModeChanged(Instant at, HomeMode before, HomeMode after) implements HomeChange {}
