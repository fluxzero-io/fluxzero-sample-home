package io.fluxzero.home.household.api.model;

import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.household.api.PlaceId;
import io.fluxzero.home.household.api.SpaceId;
import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import io.fluxzero.sdk.modeling.Parent;
import lombok.With;

/** An independently managed building, floor, room or outdoor space within a home. */
@Model
@With
public record Space(@EntityId SpaceId id,
                    @Parent(types = {Home.class, Space.class}, pathInParent = "spaces") PlaceId<?> parentId,
                    SpaceDetails details, DeviceId primaryLightId) implements Place {}
