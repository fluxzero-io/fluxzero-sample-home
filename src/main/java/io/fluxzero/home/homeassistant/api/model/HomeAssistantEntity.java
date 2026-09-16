package io.fluxzero.home.homeassistant.api.model;

import io.fluxzero.home.devices.api.model.Capability;
import io.fluxzero.home.devices.api.model.Measurement;
import java.util.Set;

/** A discovery result describes only functions this adapter actually supports. */
public record HomeAssistantEntity(String entityId, String name, Set<Capability> capabilities,
                                   Set<Measurement> measurements) {}
