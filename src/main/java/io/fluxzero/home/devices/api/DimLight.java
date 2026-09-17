package io.fluxzero.home.devices.api;

import io.fluxzero.home.devices.api.model.Device;
import io.fluxzero.home.devices.api.model.DeviceSetting;
import io.fluxzero.home.devices.api.model.LightLevel;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Choose the brightness of a light. */
public record DimLight(DeviceId deviceId, @NotNull @Valid LightLevel brightness) implements DeviceCommand {
    @Override
    public DeviceSetting setting() {
        return brightness;
    }

    @Apply
    Device apply(Device device) {
        return device.withPendingSettings(device.pendingSettings().with(brightness));
    }
}
