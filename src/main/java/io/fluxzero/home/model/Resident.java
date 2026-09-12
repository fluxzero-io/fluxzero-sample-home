package io.fluxzero.home.model;

import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import io.fluxzero.sdk.modeling.Parent;
import lombok.With;

import static io.fluxzero.sdk.modeling.ModelPersistence.DOCUMENT;
import static io.fluxzero.sdk.modeling.ModelPersistence.EVENT_SOURCED;

/** A household member with an independent profile and explicit presence. */
@Model(persistence = {EVENT_SOURCED, DOCUMENT})
@With
public record Resident(@EntityId ResidentId residentId, @Parent(pathInParent = "residents") HomeId homeId,
                       String name, HouseholdRole role, Presence presence) {}
