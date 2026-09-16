package io.fluxzero.home.scenes.api.model;

import io.fluxzero.home.devices.api.DeviceCommand;
import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.devices.api.SetFanSpeed;
import io.fluxzero.home.devices.api.model.Capability;
import io.fluxzero.home.devices.api.model.FanSpeed;
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
