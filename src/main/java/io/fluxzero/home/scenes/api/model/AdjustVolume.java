package io.fluxzero.home.scenes.api.model;

import io.fluxzero.home.devices.api.DeviceCommand;
import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.devices.api.SetVolume;
import io.fluxzero.home.devices.api.model.Capability;
import io.fluxzero.home.devices.api.model.Volume;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Choose the volume of the selected players. */
public record AdjustVolume(@NotNull @Valid SceneTarget target, @NotNull @Valid Volume volume) implements SceneAction {
    @Override
    public Capability capability() {
        return Capability.VOLUME;
    }

    @Override
    public DeviceCommand commandFor(DeviceId deviceId) {
        return new SetVolume(deviceId, volume.percent());
    }
}
