package io.fluxzero.home.command;

import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.DeviceSetting;

/** Ask a lock to unlock a door. */
public record UnlockDoor(DeviceId deviceId) implements DeviceCommand {
    public DeviceSetting setting() { return new DeviceSetting.Lock(false); }
}
