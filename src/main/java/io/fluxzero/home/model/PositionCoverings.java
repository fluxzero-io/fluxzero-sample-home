package io.fluxzero.home.model;

import io.fluxzero.home.command.DeviceCommand;
import io.fluxzero.home.command.SetOpening;
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
