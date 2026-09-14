package io.fluxzero.home.command;

import io.fluxzero.home.model.Device;
import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.DeviceSetting;
import io.fluxzero.home.model.Playback;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import jakarta.validation.Valid;

/** Choose music or another media source. */
public record PlayMedia(DeviceId deviceId, String media) implements DeviceCommand {
    @Override
    @Valid
    public DeviceSetting setting() { return new Playback(true, media); }

    @Apply
    Device apply(Device device) {
        return device.withDesiredSettings(device.desiredSettings().with(setting()));
    }
}
