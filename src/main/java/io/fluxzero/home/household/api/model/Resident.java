package io.fluxzero.home.household.api.model;

import io.fluxzero.home.household.api.HomeId;
import io.fluxzero.home.household.api.ResidentId;
import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import io.fluxzero.sdk.modeling.Parent;
import lombok.With;

/** A household member with an independent profile and explicit presence. */
@Model
@With
public record Resident(@EntityId ResidentId residentId, @Parent(pathInParent = "residents") HomeId homeId,
                       ResidentDetails details, HouseholdRole role, Presence presence) {}
