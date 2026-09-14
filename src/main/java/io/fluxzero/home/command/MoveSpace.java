package io.fluxzero.home.command;

import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.PlaceId;
import io.fluxzero.home.model.Space;
import io.fluxzero.home.model.SpaceId;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.tracking.handling.Association;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/** Move a space and its contents within the same home; relationship cycles are rejected atomically. */
public record MoveSpace(@NotNull SpaceId spaceId, @NotNull PlaceId<?> parentId) {
    @AssertTrue(message = "A space cannot contain itself.")
    boolean hasDifferentParent() {
        return !spaceId.equals(parentId);
    }

    @AssertLegal void remainsInSameHome(@Association("spaceId") Graph<Space> space) {
        var home = space.ancestor(Home.class).orElseThrow();
        if (home.find(parentId, parentId.getType()).isEmpty()) {
            throw new IllegalCommandException("Choose a destination from this home.");
        }
    }
    @Apply Space apply(@Association("spaceId") Space space) { return space.withParentId(parentId); }
}
