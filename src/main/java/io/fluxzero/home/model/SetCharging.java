package io.fluxzero.home.model;

import io.fluxzero.home.command.DeviceCommand;
import io.fluxzero.home.command.EnableCharging;
import io.fluxzero.home.command.PauseCharging;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Enable or pause charging on the selected devices. */
public record SetCharging(@NotNull @Valid SceneTarget target, @NotNull @Valid Charging charging) implements SceneAction {
    @Override
    public Capability capability() {
        return Capability.CHARGING;
    }

    @Override
    public DeviceCommand commandFor(DeviceId deviceId) {
        return charging.enabled() ? new EnableCharging(deviceId) : new PauseCharging(deviceId);
    }
}
