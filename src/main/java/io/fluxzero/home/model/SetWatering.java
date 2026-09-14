package io.fluxzero.home.model;

import io.fluxzero.home.command.DeviceCommand;
import io.fluxzero.home.command.StartWatering;
import io.fluxzero.home.command.StopWatering;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Start or stop watering in the selected areas. */
public record SetWatering(@NotNull @Valid SceneTarget target, @NotNull @Valid Irrigation irrigation) implements SceneAction {
    @Override
    public Capability capability() {
        return Capability.IRRIGATION;
    }

    @Override
    public DeviceCommand commandFor(DeviceId deviceId) {
        return irrigation.watering() ? new StartWatering(deviceId) : new StopWatering(deviceId);
    }
}
