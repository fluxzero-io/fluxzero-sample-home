package io.fluxzero.home.command;

import io.fluxzero.home.model.Device;
import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.DeviceSetting;
import io.fluxzero.home.model.LightColor;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import jakarta.validation.Valid;

/** Choose a light’s color. */
public record SetLightColor(DeviceId deviceId, int hue, int saturation) implements DeviceCommand {
    @Override
    @Valid
    public DeviceSetting setting() { return new LightColor(hue, saturation); }

    @Apply
    Device apply(Device device) {
        return device.withDesiredSettings(device.desiredSettings().with(setting()));
    }
}
