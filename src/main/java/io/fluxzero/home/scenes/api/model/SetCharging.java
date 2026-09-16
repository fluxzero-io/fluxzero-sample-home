package io.fluxzero.home.scenes.api.model;

import io.fluxzero.home.devices.api.DeviceCommand;
import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.devices.api.EnableCharging;
import io.fluxzero.home.devices.api.PauseCharging;
import io.fluxzero.home.devices.api.model.Capability;
import io.fluxzero.home.devices.api.model.Charging;
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
