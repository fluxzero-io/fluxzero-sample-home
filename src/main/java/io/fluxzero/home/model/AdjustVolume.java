package io.fluxzero.home.model;

import io.fluxzero.home.command.DeviceCommand;
import io.fluxzero.home.command.SetVolume;
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
