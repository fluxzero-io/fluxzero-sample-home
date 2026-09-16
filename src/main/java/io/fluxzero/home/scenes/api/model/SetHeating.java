package io.fluxzero.home.scenes.api.model;

import io.fluxzero.home.devices.api.DeviceCommand;
import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.devices.api.SetRoomTemperature;
import io.fluxzero.home.devices.api.model.Capability;
import io.fluxzero.home.devices.api.model.RoomTemperature;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Choose a comfort temperature for the selected climate devices. */
public record SetHeating(@NotNull @Valid SceneTarget target, @NotNull @Valid RoomTemperature temperature) implements SceneAction {
    @Override
    public Capability capability() {
        return Capability.TEMPERATURE;
    }

    @Override
    public DeviceCommand commandFor(DeviceId deviceId) {
        return new SetRoomTemperature(deviceId, temperature);
    }
}
