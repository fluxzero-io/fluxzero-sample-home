package io.fluxzero.home.homeassistant;

import io.fluxzero.home.model.DeviceId;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Stop controlling and observing this device through Home Assistant. */
public record UnlinkHomeAssistantDevice(DeviceId deviceId) {
    @Apply HomeAssistantDevice apply(HomeAssistantDevice binding) { return null; }
}
