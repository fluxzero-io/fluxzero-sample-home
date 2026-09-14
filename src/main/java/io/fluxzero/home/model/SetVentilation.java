package io.fluxzero.home.model;

import io.fluxzero.home.command.DeviceCommand;
import io.fluxzero.home.command.SetFanSpeed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Choose the strength of the selected fans. */
public record SetVentilation(@NotNull @Valid SceneTarget target, @NotNull @Valid FanSpeed speed) implements SceneAction {
    @Override
    public Capability capability() {
        return Capability.FAN_SPEED;
    }

    @Override
    public DeviceCommand commandFor(DeviceId deviceId) {
        return new SetFanSpeed(deviceId, speed.percent());
    }
}
