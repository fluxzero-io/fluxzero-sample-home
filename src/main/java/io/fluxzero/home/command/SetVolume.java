package io.fluxzero.home.command;

import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.DeviceSetting;

/** Choose a comfortable volume. */
public record SetVolume(DeviceId deviceId, int percent) implements DeviceCommand {
    public DeviceSetting setting() { return new DeviceSetting.Volume(percent); }
}
