package io.fluxzero.home.command;

import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.home.model.ScenePlan;
import io.fluxzero.home.model.Space;
import io.fluxzero.home.model.SpaceId;
import io.fluxzero.home.model.SpaceKind;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import jakarta.annotation.Nullable;

import static io.fluxzero.home.model.Rules.named;
import static io.fluxzero.home.model.Rules.require;

/** Add a building, floor, room or outdoor space, optionally inside another space. */
public record AddSpace(SpaceId spaceId, HomeId homeId, SpaceId enclosingSpaceId, String name, SpaceKind kind) {
    @AssertLegal void validate(Graph<Home> home) {
        named(name); require(kind != null, "Choose the kind of space.");
        if (enclosingSpaceId != null) ScenePlan.find(home, enclosingSpaceId, Space.class);
    }
    @AssertLegal void requireNew(@Nullable Space existing) {
        require(existing == null, "This space already exists.");
    }
    @Apply Space apply(Home home) { return new Space(spaceId, homeId, enclosingSpaceId, name, kind, null); }
}
