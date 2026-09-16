package io.fluxzero.home.scenes.api.model;

import io.fluxzero.home.devices.api.model.Capability;
import io.fluxzero.home.devices.api.model.Device;
import io.fluxzero.home.household.api.model.Home;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;
import java.util.List;

/** Select suitable devices in this home. */
public record WholeHome() implements SceneTarget {
    @Override
    public List<Device> select(Graph<Home> home, Capability capability) {
        var devices = home.descendantModels(Device.class).stream()
                .filter(device -> device.capabilities().contains(capability)).toList();
        if (devices.isEmpty()) {
            throw new IllegalCommandException("No suitable device was found in this home.");
        }
        return devices;
    }
}
