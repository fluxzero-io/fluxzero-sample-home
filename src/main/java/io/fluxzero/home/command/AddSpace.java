package io.fluxzero.home.command;

import io.fluxzero.common.serialization.Revision;
import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.home.model.Space;
import io.fluxzero.home.model.SpaceDetails;
import io.fluxzero.home.model.SpaceId;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.tracking.handling.Association;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import static io.fluxzero.home.model.Rules.require;

/** Add a building, floor, room or outdoor space, optionally inside another space. */
@Revision(1)
public record AddSpace(SpaceId spaceId, HomeId homeId, SpaceId enclosingSpaceId, @NotNull @Valid SpaceDetails details) {
    @AssertLegal void validate(Home home, @Nullable @Association("enclosingSpaceId") Space enclosingSpace) {
        require(enclosingSpaceId == null || enclosingSpace != null, "Choose an existing enclosing space.");
        require(enclosingSpace == null || enclosingSpace.homeId().equals(home.homeId()), "The enclosing space belongs to another home.");
    }
    @Apply Space apply(Home home) { return new Space(spaceId, homeId, enclosingSpaceId, details, null); }
}
