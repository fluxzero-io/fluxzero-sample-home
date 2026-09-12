package io.fluxzero.home.command;

import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.Scene;
import io.fluxzero.home.model.SceneTarget;
import io.fluxzero.home.model.Space;
import io.fluxzero.home.model.SpaceId;
import io.fluxzero.home.model.Zone;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

import static io.fluxzero.home.model.Rules.require;

/** Remove an empty, unreferenced space without silently discarding its contents or zone memberships. */
public record RemoveSpace(SpaceId spaceId) {
    @AssertLegal void validate(Graph<Space> space, Graph<Home> home) {
        require(space.children().isEmpty(), "Move or remove this space’s contents first.");
        require(home.childModels(Zone.class).stream().noneMatch(z -> z.spaces().contains(spaceId)), "Remove this space from its zones first.");
        require(home.childModels(Scene.class).stream().flatMap(s -> s.actions().stream())
                .noneMatch(a -> a.target() instanceof SceneTarget.InSpace t && t.spaceId().equals(spaceId)), "Remove this space from its scenes first.");
    }
    @Apply Space apply(Space space) { return null; }
}
