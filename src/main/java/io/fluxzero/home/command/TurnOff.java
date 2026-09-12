package io.fluxzero.home.command;

import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.DeviceSetting;

/** Turn a device off. */
public record TurnOff(DeviceId deviceId) implements DeviceCommand {
    public DeviceSetting setting() { return new DeviceSetting.Power(false); }
}
