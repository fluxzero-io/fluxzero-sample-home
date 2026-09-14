package io.fluxzero.home.command;

import io.fluxzero.home.model.Device;
import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.DeviceSetting;
import io.fluxzero.home.model.Power;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Turn a device off. */
public record TurnOff(DeviceId deviceId) implements DeviceCommand {
    @Override
    public DeviceSetting setting() { return new Power(false); }

    @Apply
    Device apply(Device device) {
        return device.withDesiredSettings(device.desiredSettings().with(setting()));
    }
}
