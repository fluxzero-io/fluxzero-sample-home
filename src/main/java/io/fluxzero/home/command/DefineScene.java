package io.fluxzero.home.command;

import io.fluxzero.home.model.DeviceSetting;
import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.home.model.Scene;
import io.fluxzero.home.model.SceneAction;
import io.fluxzero.home.model.SceneId;
import io.fluxzero.home.model.ScenePlan;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import jakarta.annotation.Nullable;
import java.util.List;

import static io.fluxzero.home.model.Rules.named;
import static io.fluxzero.home.model.Rules.require;

/** Create or revise a scene after checking every action against its home. */
public record DefineScene(SceneId sceneId, HomeId homeId, String name, List<SceneAction> actions) {
    public DefineScene { actions = List.copyOf(actions); }
    @AssertLegal List<DeviceSetting> validate(Graph<Home> home, @Nullable Scene scene) {
        named(name); require(scene == null || scene.homeId().equals(homeId), "A scene cannot move between homes.");
        ScenePlan.devices(actions, home);
        return actions.stream().map(SceneAction::setting).toList();
    }
    @Apply Scene apply(@Nullable Scene scene, Home home) {
        return new Scene(sceneId, homeId, name, actions, scene == null ? 0 : scene.activationCount(),
                scene == null ? null : scene.lastActivatedAt());
    }
}
