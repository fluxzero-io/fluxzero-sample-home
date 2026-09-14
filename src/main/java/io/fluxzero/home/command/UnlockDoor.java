package io.fluxzero.home.command;

import io.fluxzero.home.model.Device;
import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.DeviceSetting;
import io.fluxzero.home.model.DoorLock;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Ask a lock to unlock a door. */
public record UnlockDoor(DeviceId deviceId) implements DeviceCommand {
    @Override
    public DeviceSetting setting() { return new DoorLock(false); }

    @Apply
    Device apply(Device device) {
        return device.withDesiredSettings(device.desiredSettings().with(setting()));
    }
}
