package io.fluxzero.home.model;

import io.fluxzero.home.command.DeviceCommand;
import io.fluxzero.home.command.TurnOff;
import io.fluxzero.home.command.TurnOn;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Switch the selected devices on or off. */
public record SwitchPower(@NotNull @Valid SceneTarget target, @NotNull @Valid Power power) implements SceneAction {
    @Override
    public Capability capability() {
        return Capability.POWER;
    }

    @Override
    public DeviceCommand commandFor(DeviceId deviceId) {
        return power.on() ? new TurnOn(deviceId) : new TurnOff(deviceId);
    }
}
