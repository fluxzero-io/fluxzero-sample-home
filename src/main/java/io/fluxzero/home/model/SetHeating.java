package io.fluxzero.home.model;

import io.fluxzero.home.command.DeviceCommand;
import io.fluxzero.home.command.SetRoomTemperature;
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
