package io.fluxzero.home.devices.api;

import io.fluxzero.home.devices.api.model.Device;
import io.fluxzero.home.devices.api.model.DeviceSetting;
import io.fluxzero.home.devices.api.model.Volume;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import jakarta.validation.Valid;

/** Choose a comfortable volume. */
public record SetVolume(DeviceId deviceId, int percent) implements DeviceCommand {
    @Override
    @Valid
    public DeviceSetting setting() { return new Volume(percent); }

    @Apply
    Device apply(Device device) {
        return device.withDesiredSettings(device.desiredSettings().with(setting()));
    }
}
