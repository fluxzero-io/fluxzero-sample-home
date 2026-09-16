package io.fluxzero.home.scenes.api.model;

import io.fluxzero.home.devices.api.DeviceCommand;
import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.devices.api.PlayMedia;
import io.fluxzero.home.devices.api.StopMedia;
import io.fluxzero.home.devices.api.model.Capability;
import io.fluxzero.home.devices.api.model.Playback;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Start or stop media on the selected players. */
public record SetPlayback(@NotNull @Valid SceneTarget target, @NotNull @Valid Playback playback) implements SceneAction {
    @Override
    public Capability capability() {
        return Capability.PLAYBACK;
    }

    @Override
    public DeviceCommand commandFor(DeviceId deviceId) {
        return playback.playing() ? new PlayMedia(deviceId, playback.media()) : new StopMedia(deviceId);
    }
}
