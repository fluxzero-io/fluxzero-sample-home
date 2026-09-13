package io.fluxzero.home.model;

import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import io.fluxzero.sdk.modeling.Parent;
import java.util.Set;
import lombok.With;

/** An overlapping group such as downstairs or the garden; it does not own its spaces. */
@Model
@With
public record Zone(@EntityId ZoneId zoneId, @Parent(pathInParent = "zones") HomeId homeId,
                   String name, Set<SpaceId> spaces) {
    public Zone { spaces = Set.copyOf(spaces); }
}
