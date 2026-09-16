package io.fluxzero.home.devices.api.model;



/** Whether a garden area is being watered. */
public record Irrigation(boolean watering) implements DeviceSetting {
    @Override
    public Capability capability() {
        return Capability.IRRIGATION;
    }
}
