package io.fluxzero.home.devices.api.model;

import jakarta.validation.constraints.AssertTrue;

/** Whether media is playing and what is being played. */
public record Playback(boolean playing, String media) implements DeviceSetting {
    @Override
    public Capability capability() {
        return Capability.PLAYBACK;
    }

    @AssertTrue(message = "Choose what to play.")
    boolean hasMediaWhenPlaying() {
        return !playing || media != null && !media.isBlank();
    }
}
