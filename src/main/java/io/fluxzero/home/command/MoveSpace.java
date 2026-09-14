package io.fluxzero.home.command;

import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.Space;
import io.fluxzero.home.model.SpaceId;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.tracking.handling.Association;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import static io.fluxzero.home.model.Rules.require;

/** Move a space and its contents within the same home; relationship cycles are rejected atomically. */
public record MoveSpace(@NotNull SpaceId spaceId, SpaceId enclosingSpaceId) {
    @AssertTrue(message = "A space cannot contain itself.")
    boolean hasDifferentParent() {
        return spaceId == null || !spaceId.equals(enclosingSpaceId);
    }

    @AssertLegal void validate(@Association("spaceId") Space space, Graph<Home> home) {
        require(enclosingSpaceId == null || home.find(enclosingSpaceId, Space.class).isPresent(),
                "Choose an existing space from this home.");
    }
    @Apply Space apply(@Association("spaceId") Space space) { return space.withEnclosingSpaceId(enclosingSpaceId); }
}
