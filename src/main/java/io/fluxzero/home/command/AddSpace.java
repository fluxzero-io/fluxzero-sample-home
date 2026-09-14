package io.fluxzero.home.command;

import io.fluxzero.home.model.PlaceId;
import io.fluxzero.home.model.Space;
import io.fluxzero.home.model.SpaceDetails;
import io.fluxzero.home.model.SpaceId;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Add a building, floor, room or outdoor space to a home or another space. */
public record AddSpace(@NotNull SpaceId spaceId, @NotNull PlaceId<?> parentId,
                       @NotNull @Valid SpaceDetails details) {
    @AssertLegal void parentExists() {
        if (!Fluxzero.loadModel(parentId).isPresent()) {
            throw new IllegalCommandException("Choose an existing home or space.");
        }
    }

    @Apply Space apply() { return new Space(spaceId, parentId, details, null); }
}
