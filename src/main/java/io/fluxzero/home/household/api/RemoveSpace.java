package io.fluxzero.home.household.api;

import io.fluxzero.home.household.api.model.Home;
import io.fluxzero.home.household.api.model.Space;
import io.fluxzero.home.household.api.model.Zone;
import io.fluxzero.home.scenes.api.model.InSpace;
import io.fluxzero.home.scenes.api.model.Scene;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;

/** Remove an empty, unreferenced space without silently discarding its contents or zone memberships. */
public record RemoveSpace(SpaceId spaceId) {
    @AssertLegal void validate(Graph<Space> space, Graph<Home> home) {
        if (!space.children().isEmpty()) {
            throw new IllegalCommandException("Move or remove this space’s contents first.");
        }
        if (home.childModels(Zone.class).stream().anyMatch(z -> z.spaces().contains(spaceId))) {
            throw new IllegalCommandException("Remove this space from its zones first.");
        }
        if (home.childModels(Scene.class).stream().flatMap(s -> s.actions().stream())
                .anyMatch(a -> a.target() instanceof InSpace t && t.spaceId().equals(spaceId))) {
            throw new IllegalCommandException("Remove this space from its scenes first.");
        }
    }
    @Apply Space apply(Space space) { return null; }
}
