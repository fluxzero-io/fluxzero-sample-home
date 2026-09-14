package io.fluxzero.home.command;

import io.fluxzero.home.model.Capability;
import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.Scene;
import io.fluxzero.home.model.SceneId;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.persisting.eventsourcing.InterceptApply;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;

import static io.fluxzero.sdk.modeling.EventPublication.ALWAYS;

/** Execute the selected device intentions and record the activation in one atomic commit. */
public record ActivateScene(SceneId sceneId) {
    @InterceptApply
    List<Object> prepare(Scene scene, Graph<Home> home) {
        var commands = new LinkedHashMap<DeviceId, EnumMap<Capability, DeviceCommand>>();
        for (var action : scene.actions()) {
            for (var device : action.target().select(home, action.capability())) {
                commands.computeIfAbsent(device.deviceId(), ignored -> new EnumMap<>(Capability.class))
                        .put(action.capability(), action.commandFor(device.deviceId()));
            }
        }
        var result = new ArrayList<Object>();
        commands.values().forEach(perDevice -> result.addAll(perDevice.values()));
        result.add(this);
        return result;
    }

    @Apply(eventPublication = ALWAYS)
    Scene apply(Scene scene) {
        return scene;
    }
}
