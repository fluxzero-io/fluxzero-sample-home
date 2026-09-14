package io.fluxzero.home.model;

import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Select suitable devices in this space. */
public record InSpace(@NotNull SpaceId spaceId) implements SceneTarget {
    @Override
    public List<Device> select(Graph<Home> home, Capability capability) {
        var space = home.find(spaceId, Space.class).orElseThrow(
                () -> new IllegalCommandException("Choose a space from this home."));
        var devices = space.descendantModels(Device.class).stream()
                .filter(device -> device.capabilities().contains(capability)).toList();
        if (devices.isEmpty()) {
            throw new IllegalCommandException("No suitable device was found in this space.");
        }
        return devices;
    }
}
