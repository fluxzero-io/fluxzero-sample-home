package io.fluxzero.home.devices.api.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Playback volume, as a percentage. */
public record Volume(@Min(0) @Max(100) int percent) implements DeviceSetting {
    @Override
    public Capability capability() {
        return Capability.VOLUME;
    }
}
