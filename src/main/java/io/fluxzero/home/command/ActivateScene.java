package io.fluxzero.home.command;

import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.Scene;
import io.fluxzero.home.model.SceneId;
import io.fluxzero.home.model.ScenePlan;
import io.fluxzero.sdk.common.Message;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.persisting.eventsourcing.InterceptApply;
import java.util.List;

/** Expand a scene into recognizable device commands that commit together with its activation. */
public record ActivateScene(SceneId sceneId) {
    @InterceptApply List<Object> prepare(Scene scene, Graph<Home> home) {
        var commands = ScenePlan.commands(scene, home);
        commands.add(this);
        return commands;
    }
    @Apply Scene apply(Scene scene, Message message) {
        return scene.withActivationCount(scene.activationCount() + 1).withLastActivatedAt(message.getTimestamp());
    }
}
