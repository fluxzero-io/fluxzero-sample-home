package io.fluxzero.home.command;

import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.DeviceSetting;

/** Choose the brightness of a light. */
public record DimLight(DeviceId deviceId, int percent) implements DeviceCommand {
    public DeviceSetting setting() { return new DeviceSetting.LightLevel(percent); }
}
