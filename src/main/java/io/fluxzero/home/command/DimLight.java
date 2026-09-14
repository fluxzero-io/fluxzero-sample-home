package io.fluxzero.home.command;

import io.fluxzero.home.model.Device;
import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.DeviceSetting;
import io.fluxzero.home.model.LightLevel;
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
        return device.withDesiredSettings(device.desiredSettings().with(brightness));
    }
}
