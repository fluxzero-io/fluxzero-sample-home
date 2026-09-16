package io.fluxzero.home.devices.api;

import io.fluxzero.home.devices.api.model.Device;
import io.fluxzero.home.devices.api.model.DeviceSetting;
import io.fluxzero.home.devices.api.model.FanSpeed;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import jakarta.validation.Valid;

/** Choose ventilation strength. */
public record SetFanSpeed(DeviceId deviceId, int percent) implements DeviceCommand {
    @Override
    @Valid
    public DeviceSetting setting() { return new FanSpeed(percent); }

    @Apply
    Device apply(Device device) {
        return device.withDesiredSettings(device.desiredSettings().with(setting()));
    }
}
