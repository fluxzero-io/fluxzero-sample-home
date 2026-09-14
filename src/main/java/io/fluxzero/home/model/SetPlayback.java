package io.fluxzero.home.model;

import io.fluxzero.home.command.DeviceCommand;
import io.fluxzero.home.command.PlayMedia;
import io.fluxzero.home.command.StopMedia;
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
