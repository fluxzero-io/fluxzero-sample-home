package io.fluxzero.home.command;

import io.fluxzero.home.model.Device;
import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.DeviceSetting;
import io.fluxzero.home.model.Irrigation;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Stop watering a garden area. */
public record StopWatering(DeviceId deviceId) implements DeviceCommand {
    @Override
    public DeviceSetting setting() { return new Irrigation(false); }

    @Apply
    Device apply(Device device) {
        return device.withDesiredSettings(device.desiredSettings().with(setting()));
    }
}
