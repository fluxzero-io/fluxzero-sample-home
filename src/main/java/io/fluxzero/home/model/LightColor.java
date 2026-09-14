package io.fluxzero.home.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** The hue and saturation of a light. */
public record LightColor(@Min(0) @Max(359) int hue, @Min(0) @Max(100) int saturation) implements DeviceSetting {
    @Override
    public Capability capability() {
        return Capability.LIGHT_COLOR;
    }
}
