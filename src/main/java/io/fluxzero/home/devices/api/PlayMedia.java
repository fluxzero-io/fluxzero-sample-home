package io.fluxzero.home.devices.api;

import io.fluxzero.home.devices.api.model.Device;
import io.fluxzero.home.devices.api.model.DeviceSetting;
import io.fluxzero.home.devices.api.model.Playback;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import jakarta.validation.Valid;

/** Choose music or another media source. */
public record PlayMedia(DeviceId deviceId, String media) implements DeviceCommand {
    @Override
    @Valid
    public DeviceSetting setting() { return new Playback(true, media); }

    @Apply
    Device apply(Device device) {
        return device.withPendingSettings(device.pendingSettings().with(setting()));
    }
}
