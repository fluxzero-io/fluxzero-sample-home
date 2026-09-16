package io.fluxzero.home.household.api;

import io.fluxzero.home.household.api.model.Home;
import io.fluxzero.home.household.api.model.Space;
import io.fluxzero.home.household.api.model.Zone;
import io.fluxzero.home.household.api.model.ZoneDetails;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

/** Group spaces without changing their place in the home. */
public record DefineZone(ZoneId zoneId, HomeId homeId, @NotNull @Valid ZoneDetails details,
                         @NotEmpty(message = "A zone needs at least one space.") Set<@NotNull SpaceId> spaces) {
    @AssertLegal void validate(Graph<Home> home, @Nullable Zone zone) {
        if (zone != null && !zone.homeId().equals(homeId)) {
            throw new IllegalCommandException("A zone cannot move between homes.");
        }
        if (spaces.stream().anyMatch(id -> home.find(id, Space.class).isEmpty())) {
            throw new IllegalCommandException("Choose an existing space from this home.");
        }
    }
    @Apply Zone apply(@Nullable Zone zone, Home home) { return new Zone(zoneId, homeId, details, spaces); }
}
