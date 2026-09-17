package io.fluxzero.home.devices.api;

import io.fluxzero.home.devices.api.model.Device;
import io.fluxzero.home.devices.api.model.DeviceSetting;
import io.fluxzero.home.devices.api.model.LightColor;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import jakarta.validation.Valid;

/** Choose a light’s color. */
public record SetLightColor(DeviceId deviceId, int hue, int saturation) implements DeviceCommand {
    @Override
    @Valid
    public DeviceSetting setting() { return new LightColor(hue, saturation); }

    @Apply
    Device apply(Device device) {
        return device.withPendingSettings(device.pendingSettings().with(setting()));
    }
}
