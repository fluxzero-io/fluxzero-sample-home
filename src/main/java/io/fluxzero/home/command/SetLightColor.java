package io.fluxzero.home.command;

import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.DeviceSetting;

/** Choose a light’s color. */
public record SetLightColor(DeviceId deviceId, int hue, int saturation) implements DeviceCommand {
    public DeviceSetting setting() { return new DeviceSetting.LightColor(hue, saturation); }
}
