package io.fluxzero.home.command;

import io.fluxzero.home.model.Space;
import io.fluxzero.home.model.SpaceId;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

import static io.fluxzero.home.model.Rules.named;

/** Give a space a more useful name. */
public record RenameSpace(SpaceId spaceId, String name) {
    @AssertLegal void validate() { named(name); }
    @Apply Space apply(Space space) { return space.withDetails(space.details().withName(name)); }
}
