package io.fluxzero.home.command;

import io.fluxzero.home.model.Automation;
import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.Routine;
import io.fluxzero.home.model.Scene;
import io.fluxzero.home.model.SceneId;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;

/** Remove a scene once routines and automations no longer refer to it. */
public record RemoveScene(SceneId sceneId) {
    @AssertLegal void validate(Scene scene, Graph<Home> home) {
        if (home.childModels(Routine.class).stream().anyMatch(r -> r.sceneId().equals(sceneId))) {
            throw new IllegalCommandException("Remove this scene from its routines first.");
        }
        if (home.childModels(Automation.class).stream().anyMatch(a -> a.sceneId().equals(sceneId))) {
            throw new IllegalCommandException("Remove this scene from its automations first.");
        }
    }
    @Apply Scene apply(Scene scene) { return null; }
}
