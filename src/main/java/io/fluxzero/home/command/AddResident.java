package io.fluxzero.home.command;

import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.home.model.HouseholdRole;
import io.fluxzero.home.model.Presence;
import io.fluxzero.home.model.Resident;
import io.fluxzero.home.model.ResidentDetails;
import io.fluxzero.home.model.ResidentId;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Introduce someone who belongs to the household. */
public record AddResident(ResidentId residentId, HomeId homeId, @NotNull @Valid ResidentDetails details,
                          @NotNull(message = "Choose a household role.") HouseholdRole role) {
    @Apply Resident apply(Home home) { return new Resident(residentId, homeId, details, role, Presence.UNKNOWN); }
}
