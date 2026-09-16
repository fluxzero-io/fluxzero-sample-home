package io.fluxzero.home.scenes.api.model;

import io.fluxzero.home.devices.api.DeviceCommand;
import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.devices.api.SetLightColor;
import io.fluxzero.home.devices.api.model.Capability;
import io.fluxzero.home.devices.api.model.LightColor;
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
