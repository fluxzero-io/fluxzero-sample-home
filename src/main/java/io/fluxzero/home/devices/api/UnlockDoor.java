package io.fluxzero.home.devices.api;

import io.fluxzero.home.devices.api.model.Device;
import io.fluxzero.home.devices.api.model.DeviceSetting;
import io.fluxzero.home.devices.api.model.DoorLock;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Ask a lock to unlock a door. */
public record UnlockDoor(DeviceId deviceId) implements DeviceCommand {
    @Override
    public DeviceSetting setting() { return new DoorLock(false); }

    @Apply
    Device apply(Device device) {
        return device.withPendingSettings(device.pendingSettings().with(setting()));
    }
}
