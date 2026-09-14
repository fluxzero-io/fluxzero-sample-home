package io.fluxzero.home.model;

import io.fluxzero.home.command.DeviceCommand;
import io.fluxzero.home.command.LockDoor;
import io.fluxzero.home.command.UnlockDoor;
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
