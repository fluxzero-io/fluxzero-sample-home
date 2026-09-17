package io.fluxzero.home.devices.api;

import io.fluxzero.home.devices.api.model.Device;
import io.fluxzero.home.devices.api.model.DeviceSetting;
import io.fluxzero.home.devices.api.model.RoomTemperature;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Ask a climate device for a comfortable room temperature. */
public record SetRoomTemperature(DeviceId deviceId, @NotNull @Valid RoomTemperature temperature) implements DeviceCommand {
    @Override
    public DeviceSetting setting() {
        return temperature;
    }

    @Apply
    Device apply(Device device) {
        return device.withPendingSettings(device.pendingSettings().with(temperature));
    }
}
