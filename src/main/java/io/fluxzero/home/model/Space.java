package io.fluxzero.home.model;

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
