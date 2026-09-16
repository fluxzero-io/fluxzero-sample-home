package io.fluxzero.home.scenes.api.model;

import io.fluxzero.home.devices.api.DeviceCommand;
import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.devices.api.TurnOff;
import io.fluxzero.home.devices.api.TurnOn;
import io.fluxzero.home.devices.api.model.Capability;
import io.fluxzero.home.devices.api.model.Power;
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
