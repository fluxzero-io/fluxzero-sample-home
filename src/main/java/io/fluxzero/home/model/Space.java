package io.fluxzero.home.model;

import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Id;
import io.fluxzero.sdk.modeling.Model;
import io.fluxzero.sdk.modeling.Parent;
import lombok.With;

import static io.fluxzero.sdk.modeling.ModelPersistence.DOCUMENT;
import static io.fluxzero.sdk.modeling.ModelPersistence.EVENT_SOURCED;

/** An independently managed building, floor, room or outdoor space within a home. */
@Model(persistence = {EVENT_SOURCED, DOCUMENT})
@With
public record Space(@EntityId SpaceId spaceId, HomeId homeId, SpaceId enclosingSpaceId,
                    String name, SpaceKind kind, DeviceId primaryLightId) {
    @Parent(types = {Home.class, Space.class}, pathInParent = "spaces")
    public Id<?> parentId() { return enclosingSpaceId == null ? homeId : enclosingSpaceId; }
}
