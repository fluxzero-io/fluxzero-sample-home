package io.fluxzero.home.command;

import io.fluxzero.home.model.Device;
import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.DeviceSetting;
import io.fluxzero.home.model.FanSpeed;
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
