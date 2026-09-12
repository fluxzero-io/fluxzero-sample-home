package io.fluxzero.home.command;

import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.DeviceSetting;

/** Ask a lock to secure a door. */
public record LockDoor(DeviceId deviceId) implements DeviceCommand {
    public DeviceSetting setting() { return new DeviceSetting.Lock(true); }
}
