package io.fluxzero.home.devices.api;

import io.fluxzero.home.devices.api.model.Charging;
import io.fluxzero.home.devices.api.model.Device;
import io.fluxzero.home.devices.api.model.DeviceSetting;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Pause charging. */
public record PauseCharging(DeviceId deviceId) implements DeviceCommand {
    @Override
    public DeviceSetting setting() { return new Charging(false); }

    @Apply
    Device apply(Device device) {
        return device.withPendingSettings(device.pendingSettings().with(setting()));
    }
}
