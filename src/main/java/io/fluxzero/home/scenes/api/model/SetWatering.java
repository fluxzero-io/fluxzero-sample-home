package io.fluxzero.home.scenes.api.model;

import io.fluxzero.home.devices.api.DeviceCommand;
import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.devices.api.StartWatering;
import io.fluxzero.home.devices.api.StopWatering;
import io.fluxzero.home.devices.api.model.Capability;
import io.fluxzero.home.devices.api.model.Irrigation;
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
