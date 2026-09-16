package io.fluxzero.home.household.api;

import io.fluxzero.home.household.api.model.Presence;
import io.fluxzero.home.household.api.model.Resident;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Let the home know that a resident has left. */
public record LeaveHome(ResidentId residentId) {
    @Apply Resident apply(Resident resident) { return resident.withPresence(Presence.AWAY); }
}
