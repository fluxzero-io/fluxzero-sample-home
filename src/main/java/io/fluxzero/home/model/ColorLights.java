package io.fluxzero.home.model;

import io.fluxzero.home.command.DeviceCommand;
import io.fluxzero.home.command.SetLightColor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Choose the color of the selected lights. */
public record ColorLights(@NotNull @Valid SceneTarget target, @NotNull @Valid LightColor color) implements SceneAction {
    @Override
    public Capability capability() {
        return Capability.LIGHT_COLOR;
    }

    @Override
    public DeviceCommand commandFor(DeviceId deviceId) {
        return new SetLightColor(deviceId, color.hue(), color.saturation());
    }
}
