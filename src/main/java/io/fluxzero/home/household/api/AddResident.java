package io.fluxzero.home.household.api;

import io.fluxzero.home.household.api.model.Home;
import io.fluxzero.home.household.api.model.HouseholdRole;
import io.fluxzero.home.household.api.model.Presence;
import io.fluxzero.home.household.api.model.Resident;
import io.fluxzero.home.household.api.model.ResidentDetails;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Introduce someone who belongs to the household. */
public record AddResident(ResidentId residentId, HomeId homeId, @NotNull @Valid ResidentDetails details,
                          @NotNull(message = "Choose a household role.") HouseholdRole role) {
    @Apply Resident apply(Home home) { return new Resident(residentId, homeId, details, role, Presence.UNKNOWN); }
}
