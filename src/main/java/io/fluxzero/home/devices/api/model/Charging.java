package io.fluxzero.home.devices.api.model;



/** Whether charging is enabled. */
public record Charging(boolean enabled) implements DeviceSetting {
    @Override
    public Capability capability() {
        return Capability.CHARGING;
    }
}
