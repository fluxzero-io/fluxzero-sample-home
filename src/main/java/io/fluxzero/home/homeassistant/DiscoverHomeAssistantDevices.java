package io.fluxzero.home.homeassistant;

import io.fluxzero.sdk.tracking.handling.Request;

import java.util.List;

/** Preview supported entities and their capabilities without creating household devices. */
public record DiscoverHomeAssistantDevices(HomeAssistantId connectionId) implements Request<List<HomeAssistantEntity>> {}
