package io.fluxzero.home.command;

import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.DeviceSetting;

/** Stop media playback. */
public record StopMedia(DeviceId deviceId) implements DeviceCommand {
    public DeviceSetting setting() { return new DeviceSetting.Playback(false, null); }
}
