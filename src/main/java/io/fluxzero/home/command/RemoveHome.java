package io.fluxzero.home.command;

import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Logically remove a home together with its owned spaces, devices, scenes and routines. */
public record RemoveHome(HomeId homeId) {
    @Apply Home apply(Home home) { return null; }
}
