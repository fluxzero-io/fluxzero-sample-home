package io.fluxzero.home.scenes.api.model;

import io.fluxzero.home.devices.api.DeviceCommand;
import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.devices.api.LockDoor;
import io.fluxzero.home.devices.api.UnlockDoor;
import io.fluxzero.home.devices.api.model.Capability;
import io.fluxzero.home.devices.api.model.DoorLock;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Lock or unlock the selected doors. */
public record SetLocks(@NotNull @Valid SceneTarget target, @NotNull @Valid DoorLock lock) implements SceneAction {
    @Override
    public Capability capability() {
        return Capability.LOCK;
    }

    @Override
    public DeviceCommand commandFor(DeviceId deviceId) {
        return lock.locked() ? new LockDoor(deviceId) : new UnlockDoor(deviceId);
    }
}
