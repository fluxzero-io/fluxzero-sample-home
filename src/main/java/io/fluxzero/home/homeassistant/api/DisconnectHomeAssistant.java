package io.fluxzero.home.homeassistant.api;

import io.fluxzero.home.homeassistant.api.model.HomeAssistantConnection;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Stop this connection and remove its bindings, while keeping the household's devices. */
public record DisconnectHomeAssistant(HomeAssistantId connectionId) {
    @Apply HomeAssistantConnection apply(HomeAssistantConnection connection) { return null; }
}
