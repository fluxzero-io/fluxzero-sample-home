package io.fluxzero.home.devices.api;

import io.fluxzero.home.devices.api.model.Device;
import io.fluxzero.home.devices.api.model.DeviceSetting;
import io.fluxzero.home.devices.api.model.Opening;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import jakarta.validation.Valid;

/** Choose how far a blind, curtain or window should open. */
public record SetOpening(DeviceId deviceId, int percent) implements DeviceCommand {
    @Override
    @Valid
    public DeviceSetting setting() { return new Opening(percent); }

    @Apply
    Device apply(Device device) {
        return device.withDesiredSettings(device.desiredSettings().with(setting()));
    }
}
