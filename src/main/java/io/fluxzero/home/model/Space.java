package io.fluxzero.home.model;

import io.fluxzero.common.serialization.Revision;
import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Id;
import io.fluxzero.sdk.modeling.Model;
import io.fluxzero.sdk.modeling.Parent;
import lombok.With;

/** An independently managed building, floor, room or outdoor space within a home. */
@Model
@Revision(1)
@With
public record Space(@EntityId SpaceId spaceId, HomeId homeId, SpaceId enclosingSpaceId,
                    SpaceDetails details, DeviceId primaryLightId) {
    @Parent(types = {Home.class, Space.class}, pathInParent = "spaces")
    public Id<?> parentId() { return enclosingSpaceId == null ? homeId : enclosingSpaceId; }
}
