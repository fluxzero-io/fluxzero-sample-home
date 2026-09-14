package io.fluxzero.home.model;

/** Whether a door is locked. */
public record DoorLock(boolean locked) implements DeviceSetting {
    @Override
    public Capability capability() {
        return Capability.LOCK;
    }
}
