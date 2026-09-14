package io.fluxzero.home.command;

import io.fluxzero.home.model.Charging;
import io.fluxzero.home.model.Device;
import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.DeviceSetting;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Pause charging. */
public record PauseCharging(DeviceId deviceId) implements DeviceCommand {
    @Override
    public DeviceSetting setting() { return new Charging(false); }

    @Apply
    Device apply(Device device) {
        return device.withDesiredSettings(device.desiredSettings().with(setting()));
    }
}
