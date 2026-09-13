package io.fluxzero.home.command;

import io.fluxzero.common.serialization.Revision;
import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.home.model.HouseholdRole;
import io.fluxzero.home.model.Presence;
import io.fluxzero.home.model.Resident;
import io.fluxzero.home.model.ResidentDetails;
import io.fluxzero.home.model.ResidentId;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import static io.fluxzero.home.model.Rules.require;

/** Introduce someone who belongs to the household. */
@Revision(1)
public record AddResident(ResidentId residentId, HomeId homeId, @NotNull @Valid ResidentDetails details, HouseholdRole role) {
    @AssertLegal void validate(Home home) { require(role != null, "Choose a household role."); }
    @Apply Resident apply(Home home) { return new Resident(residentId, homeId, details, role, Presence.UNKNOWN); }
}
