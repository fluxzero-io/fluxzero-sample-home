package io.fluxzero.home.household.api;

import io.fluxzero.home.household.api.model.Space;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Give a space a more useful name. */
public record RenameSpace(SpaceId spaceId, @NotBlank @Size(max = 120) String name) {
    @Apply Space apply(Space space) { return space.withDetails(space.details().withName(name)); }
}
