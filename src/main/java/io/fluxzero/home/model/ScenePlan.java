package io.fluxzero.home.model;

import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.modeling.Id;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Resolves a scene against one pinned home Graph before any intentions are applied. */
public final class ScenePlan {
    private ScenePlan() {}
    public static List<Device> devices(List<SceneAction> actions, Graph<Home> home) {
        Rules.require(!actions.isEmpty(), "A scene needs at least one action.");
        var updated = new LinkedHashMap<DeviceId, Device>();
        for (var action : actions) {
            action.setting().validate();
            var candidates = targets(action.target(), home);
            if (!(action.target() instanceof SceneTarget.OneDevice))
                candidates = candidates.stream().filter(d -> d.capabilities().contains(action.setting().capability())).toList();
            Rules.require(!candidates.isEmpty(), "No suitable device was found for a scene action.");
            for (var device : candidates) {
                var current = updated.getOrDefault(device.deviceId(), device);
                updated.put(device.deviceId(), current.request(action.setting()));
            }
        }
        return List.copyOf(updated.values());
    }
    public static List<Object> commands(Scene scene, Graph<Home> home) {
        Rules.require(scene.homeId().equals(home.get().homeId()), "This scene belongs to another home.");
        var result = new ArrayList<Object>();
        for (var device : devices(scene.actions(), home)) {
            var before = find(home, device.deviceId(), Device.class).get();
            // Enum declaration order gives a stable order across machines and event replays.
            for (var capability : Capability.values()) {
                var setting = device.desiredSettings().get(capability);
                if (setting != null && !setting.equals(before.desiredSettings().get(capability)))
                    result.add(io.fluxzero.home.command.DeviceCommand.from(device.deviceId(), setting));
            }
        }
        return result;
    }
    private static List<Device> targets(SceneTarget target, Graph<Home> home) {
        return switch (target) {
            case SceneTarget.OneDevice one -> List.of(find(home, one.deviceId(), Device.class).get());
            case SceneTarget.InSpace space -> find(home, space.spaceId(), Space.class).descendantModels(Device.class);
            case SceneTarget.InZone zone -> find(home, zone.zoneId(), Zone.class).get().spaces().stream().sorted()
                    .flatMap(id -> find(home, id, Space.class).descendantModels(Device.class).stream())
                    .collect(java.util.stream.Collectors.toMap(Device::deviceId, d -> d, (a,b) -> a, LinkedHashMap::new))
                    .values().stream().toList();
            case SceneTarget.WholeHome ignored -> home.descendantModels(Device.class);
        };
    }
    public static <T> Graph<T> find(Graph<Home> home, Id<T> id, Class<T> type) {
        Rules.require(id != null, "Choose an existing " + type.getSimpleName() + ".");
        return home.find(id, type).orElseThrow(() -> new HomeRuleViolation("This " + type.getSimpleName() + " does not belong to this home."));
    }
}
