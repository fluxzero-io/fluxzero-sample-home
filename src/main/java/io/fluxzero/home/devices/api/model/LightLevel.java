package io.fluxzero.home.devices.api.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** The brightness of a light, as a percentage. */
public record LightLevel(@Min(0) @Max(100) int percent) implements DeviceSetting {
    @Override
    public Capability capability() {
        return Capability.LIGHT_LEVEL;
    }
}
