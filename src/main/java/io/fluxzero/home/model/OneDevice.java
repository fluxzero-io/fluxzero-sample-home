package io.fluxzero.home.model;

import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Select one device that supports the requested capability. */
public record OneDevice(@NotNull DeviceId deviceId) implements SceneTarget {
    @Override
    public List<Device> select(Graph<Home> home, Capability capability) {
        var device = home.find(deviceId, Device.class).orElseThrow(
                () -> new IllegalCommandException("Choose a device from this home.")).get();
        if (!device.capabilities().contains(capability)) {
            throw new IllegalCommandException(device.details().name() + " does not support " + capability + ".");
        }
        return List.of(device);
    }
}
