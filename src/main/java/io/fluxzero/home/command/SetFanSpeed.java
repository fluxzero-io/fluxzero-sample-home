package io.fluxzero.home.command;

import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.DeviceSetting;

/** Choose ventilation strength. */
public record SetFanSpeed(DeviceId deviceId, int percent) implements DeviceCommand {
    public DeviceSetting setting() { return new DeviceSetting.FanSpeed(percent); }
}
