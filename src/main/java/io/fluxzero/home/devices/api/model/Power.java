package io.fluxzero.home.devices.api.model;



/** Whether a device should be switched on. */
public record Power(boolean on) implements DeviceSetting {
    @Override
    public Capability capability() {
        return Capability.POWER;
    }
}
