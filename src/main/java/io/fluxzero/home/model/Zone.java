package io.fluxzero.home.model;

import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import io.fluxzero.sdk.modeling.Parent;
import java.util.Set;
import lombok.With;

import static io.fluxzero.sdk.modeling.ModelPersistence.DOCUMENT;
import static io.fluxzero.sdk.modeling.ModelPersistence.EVENT_SOURCED;

/** An overlapping group such as downstairs or the garden; it does not own its spaces. */
@Model(persistence = {EVENT_SOURCED, DOCUMENT})
@With
public record Zone(@EntityId ZoneId zoneId, @Parent(pathInParent = "zones") HomeId homeId,
                   String name, Set<SpaceId> spaces) {
    public Zone { spaces = Set.copyOf(spaces); }
}
