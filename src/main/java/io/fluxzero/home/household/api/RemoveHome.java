package io.fluxzero.home.household.api;

import io.fluxzero.home.household.api.model.Home;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Logically remove a home together with its owned spaces, devices, scenes and routines. */
public record RemoveHome(HomeId homeId) {
    @Apply Home apply(Home home) { return null; }
}
