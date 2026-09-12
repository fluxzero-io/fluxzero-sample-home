package io.fluxzero.home.command;

import io.fluxzero.home.model.Automation;
import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.Routine;
import io.fluxzero.home.model.Scene;
import io.fluxzero.home.model.SceneId;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

import static io.fluxzero.home.model.Rules.require;

/** Remove a scene once routines and automations no longer refer to it. */
public record RemoveScene(SceneId sceneId) {
    @AssertLegal void validate(Scene scene, Graph<Home> home) {
        require(home.childModels(Routine.class).stream().noneMatch(r -> r.sceneId().equals(sceneId)), "Remove this scene from its routines first.");
        require(home.childModels(Automation.class).stream().noneMatch(a -> a.sceneId().equals(sceneId)), "Remove this scene from its automations first.");
    }
    @Apply Scene apply(Scene scene) { return null; }
}
