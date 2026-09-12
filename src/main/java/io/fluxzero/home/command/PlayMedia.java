package io.fluxzero.home.command;

import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.DeviceSetting;

/** Choose music or another media source. */
public record PlayMedia(DeviceId deviceId, String media) implements DeviceCommand {
    public DeviceSetting setting() { return new DeviceSetting.Playback(true, media); }
}
