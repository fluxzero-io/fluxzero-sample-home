package io.fluxzero.home.command;

import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.home.model.HouseholdRole;
import io.fluxzero.home.model.Presence;
import io.fluxzero.home.model.Resident;
import io.fluxzero.home.model.ResidentId;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

import static io.fluxzero.home.model.Rules.named;
import static io.fluxzero.home.model.Rules.require;

/** Introduce someone who belongs to the household. */
public record AddResident(ResidentId residentId, HomeId homeId, String name, HouseholdRole role) {
    @AssertLegal void validate(Home home) { named(name); require(role != null, "Choose a household role."); }
    @Apply Resident apply(Home home) { return new Resident(residentId, homeId, name, role, Presence.UNKNOWN); }
}
