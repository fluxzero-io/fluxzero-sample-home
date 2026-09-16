package io.fluxzero.home.scenes.api.model;

import io.fluxzero.home.devices.api.DeviceCommand;
import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.devices.api.DimLight;
import io.fluxzero.home.devices.api.model.Capability;
import io.fluxzero.home.devices.api.model.LightLevel;
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
