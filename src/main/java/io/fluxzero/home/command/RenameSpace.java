package io.fluxzero.home.command;

import io.fluxzero.home.model.Space;
import io.fluxzero.home.model.SpaceId;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Give a space a more useful name. */
public record RenameSpace(SpaceId spaceId, @NotBlank @Size(max = 120) String name) {
    @Apply Space apply(Space space) { return space.withDetails(space.details().withName(name)); }
}
