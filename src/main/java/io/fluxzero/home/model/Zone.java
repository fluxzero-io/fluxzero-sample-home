package io.fluxzero.home.model;

import io.fluxzero.common.serialization.Revision;
import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import io.fluxzero.sdk.modeling.Parent;
import java.util.Set;
import lombok.With;

/** An overlapping group such as downstairs or the garden; it does not own its spaces. */
@Model
@Revision(1)
@With
public record Zone(@EntityId ZoneId zoneId, @Parent(pathInParent = "zones") HomeId homeId,
                   ZoneDetails details, Set<SpaceId> spaces) {
    public Zone { spaces = Set.copyOf(spaces); }
}
