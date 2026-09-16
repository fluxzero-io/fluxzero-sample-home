package io.fluxzero.home.devices.api;

import io.fluxzero.home.devices.api.model.Device;
import io.fluxzero.home.devices.api.model.DeviceSetting;
import io.fluxzero.home.devices.api.model.Power;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Turn a device on. */
public record TurnOn(DeviceId deviceId) implements DeviceCommand {
    @Override
    public DeviceSetting setting() { return new Power(true); }

    @Apply
    Device apply(Device device) {
        return device.withDesiredSettings(device.desiredSettings().with(setting()));
    }
}
