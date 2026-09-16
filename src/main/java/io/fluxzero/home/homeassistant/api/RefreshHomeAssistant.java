package io.fluxzero.home.homeassistant.api;

import io.fluxzero.sdk.modeling.Parent;

/** Fetch a full snapshot. Its next refresh belongs to the installation's lifecycle. */
public record RefreshHomeAssistant(@Parent HomeAssistantId connectionId) {}
