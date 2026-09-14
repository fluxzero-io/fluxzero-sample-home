package io.fluxzero.home.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Ventilation strength, as a percentage. */
public record FanSpeed(@Min(0) @Max(100) int percent) implements DeviceSetting {
    @Override
    public Capability capability() {
        return Capability.FAN_SPEED;
    }
}
