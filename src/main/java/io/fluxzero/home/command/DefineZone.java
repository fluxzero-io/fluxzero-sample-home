package io.fluxzero.home.command;

import io.fluxzero.common.serialization.Revision;
import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.home.model.ScenePlan;
import io.fluxzero.home.model.Space;
import io.fluxzero.home.model.SpaceId;
import io.fluxzero.home.model.Zone;
import io.fluxzero.home.model.ZoneDetails;
import io.fluxzero.home.model.ZoneId;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

import static io.fluxzero.home.model.Rules.require;

/** Group spaces without changing their place in the home. */
@Revision(1)
public record DefineZone(io.fluxzero.home.model.ZoneId zoneId, HomeId homeId, @NotNull @Valid ZoneDetails details, Set<SpaceId> spaces) {
    public DefineZone { spaces = Set.copyOf(spaces); }
    @AssertLegal void validate(Graph<Home> home, @Nullable Zone zone) {
        require(!spaces.isEmpty(), "A zone needs at least one space.");
        require(zone == null || zone.homeId().equals(homeId), "A zone cannot move between homes.");
        spaces.forEach(id -> ScenePlan.find(home, id, Space.class));
    }
    @Apply Zone apply(@Nullable Zone zone, Home home) { return new Zone(zoneId, homeId, details, spaces); }
}
