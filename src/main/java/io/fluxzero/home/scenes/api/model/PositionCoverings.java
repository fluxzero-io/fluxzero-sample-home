package io.fluxzero.home.scenes.api.model;

import io.fluxzero.home.devices.api.DeviceCommand;
import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.devices.api.SetOpening;
import io.fluxzero.home.devices.api.model.Capability;
import io.fluxzero.home.devices.api.model.Opening;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Choose how far the selected blinds, curtains or windows open. */
public record PositionCoverings(@NotNull @Valid SceneTarget target, @NotNull @Valid Opening opening) implements SceneAction {
    @Override
    public Capability capability() {
        return Capability.OPENING;
    }

    @Override
    public DeviceCommand commandFor(DeviceId deviceId) {
        return new SetOpening(deviceId, opening.percent());
    }
}
