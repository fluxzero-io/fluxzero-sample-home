package io.fluxzero.home.devices.api;

import io.fluxzero.home.devices.api.model.Device;
import io.fluxzero.home.devices.api.model.DeviceSetting;
import io.fluxzero.home.devices.api.model.DoorLock;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Ask a lock to secure a door. */
public record LockDoor(DeviceId deviceId) implements DeviceCommand {
    @Override
    public DeviceSetting setting() { return new DoorLock(true); }

    @Apply
    Device apply(Device device) {
        return device.withDesiredSettings(device.desiredSettings().with(setting()));
    }
}
