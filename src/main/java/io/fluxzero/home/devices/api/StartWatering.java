package io.fluxzero.home.devices.api;

import io.fluxzero.home.devices.api.model.Device;
import io.fluxzero.home.devices.api.model.DeviceSetting;
import io.fluxzero.home.devices.api.model.Irrigation;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Start watering a garden area. */
public record StartWatering(DeviceId deviceId) implements DeviceCommand {
    @Override
    public DeviceSetting setting() { return new Irrigation(true); }

    @Apply
    Device apply(Device device) {
        return device.withDesiredSettings(device.desiredSettings().with(setting()));
    }
}
