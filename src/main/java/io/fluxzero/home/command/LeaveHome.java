package io.fluxzero.home.command;

import io.fluxzero.home.model.Presence;
import io.fluxzero.home.model.Resident;
import io.fluxzero.home.model.ResidentId;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Let the home know that a resident has left. */
public record LeaveHome(ResidentId residentId) {
    @Apply Resident apply(Resident resident) { return resident.withPresence(Presence.AWAY); }
}
