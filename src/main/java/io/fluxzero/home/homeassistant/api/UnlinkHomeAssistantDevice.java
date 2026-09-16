package io.fluxzero.home.homeassistant.api;

import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.homeassistant.api.model.HomeAssistantDevice;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Stop controlling and observing this device through Home Assistant. */
public record UnlinkHomeAssistantDevice(DeviceId deviceId) {
    @Apply HomeAssistantDevice apply(HomeAssistantDevice binding) { return null; }
}
