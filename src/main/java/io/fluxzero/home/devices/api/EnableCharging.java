package io.fluxzero.home.devices.api;

import io.fluxzero.home.devices.api.model.Charging;
import io.fluxzero.home.devices.api.model.Device;
import io.fluxzero.home.devices.api.model.DeviceSetting;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Allow charging. */
public record EnableCharging(DeviceId deviceId) implements DeviceCommand {
    @Override
    public DeviceSetting setting() { return new Charging(true); }

    @Apply
    Device apply(Device device) {
        return device.withDesiredSettings(device.desiredSettings().with(setting()));
    }
}
