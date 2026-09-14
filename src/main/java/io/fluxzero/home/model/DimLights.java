package io.fluxzero.home.model;

import io.fluxzero.home.command.DeviceCommand;
import io.fluxzero.home.command.DimLight;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Choose the brightness of the selected lights. */
public record DimLights(@NotNull @Valid SceneTarget target, @NotNull @Valid LightLevel brightness) implements SceneAction {
    @Override
    public Capability capability() {
        return Capability.LIGHT_LEVEL;
    }

    @Override
    public DeviceCommand commandFor(DeviceId deviceId) {
        return new DimLight(deviceId, brightness);
    }
}
