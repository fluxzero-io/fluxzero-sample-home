package io.fluxzero.home.devices.api;

import io.fluxzero.home.devices.api.model.Device;
import io.fluxzero.home.devices.api.model.DeviceSetting;
import io.fluxzero.home.devices.api.model.Playback;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Stop media playback. */
public record StopMedia(DeviceId deviceId) implements DeviceCommand {
    @Override
    public DeviceSetting setting() { return new Playback(false, null); }

    @Apply
    Device apply(Device device) {
        return device.withDesiredSettings(device.desiredSettings().with(setting()));
    }
}
