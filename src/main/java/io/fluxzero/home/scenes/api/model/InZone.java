package io.fluxzero.home.scenes.api.model;

import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.devices.api.model.Capability;
import io.fluxzero.home.devices.api.model.Device;
import io.fluxzero.home.household.api.ZoneId;
import io.fluxzero.home.household.api.model.Home;
import io.fluxzero.home.household.api.model.Space;
import io.fluxzero.home.household.api.model.Zone;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;
import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashMap;
import java.util.List;

/** Select suitable devices in this zone. */
public record InZone(@NotNull ZoneId zoneId) implements SceneTarget {
    @Override
    public List<Device> select(Graph<Home> home, Capability capability) {
        var zone = home.find(zoneId, Zone.class).orElseThrow(
                () -> new IllegalCommandException("Choose a zone from this home.")).get();
        var devices = new LinkedHashMap<DeviceId, Device>();
        for (var spaceId : zone.spaces().stream().sorted().toList()) {
            var space = home.find(spaceId, Space.class).orElseThrow(
                    () -> new IllegalCommandException("A space in this zone no longer exists."));
            space.descendantModels(Device.class).stream()
                    .filter(device -> device.capabilities().contains(capability))
                    .forEach(device -> devices.putIfAbsent(device.deviceId(), device));
        }
        if (devices.isEmpty()) {
            throw new IllegalCommandException("No suitable device was found in this zone.");
        }
        return List.copyOf(devices.values());
    }
}
