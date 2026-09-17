package io.fluxzero.home.devices.api;

import io.fluxzero.home.devices.api.model.Device;
import io.fluxzero.home.devices.api.model.DeviceSetting;
import io.fluxzero.home.devices.api.model.Irrigation;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Stop watering a garden area. */
public record StopWatering(DeviceId deviceId) implements DeviceCommand {
    @Override
    public DeviceSetting setting() { return new Irrigation(false); }

    @Apply
    Device apply(Device device) {
        return device.withPendingSettings(device.pendingSettings().with(setting()));
    }
}
