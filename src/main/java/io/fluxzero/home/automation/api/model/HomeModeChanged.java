package io.fluxzero.home.automation.api.model;

import io.fluxzero.home.household.api.model.HomeMode;
import java.time.Instant;

/** The household mode before and after one committed change. */
public record HomeModeChanged(Instant at, HomeMode before, HomeMode after) implements HomeChange {}
