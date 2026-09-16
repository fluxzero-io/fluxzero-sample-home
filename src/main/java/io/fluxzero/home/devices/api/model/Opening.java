package io.fluxzero.home.devices.api.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** How far a blind, curtain or window is open. */
public record Opening(@Min(0) @Max(100) int percent) implements DeviceSetting {
    @Override
    public Capability capability() {
        return Capability.OPENING;
    }
}
