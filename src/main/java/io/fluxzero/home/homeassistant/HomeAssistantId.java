package io.fluxzero.home.homeassistant;

import io.fluxzero.sdk.modeling.Id;

/** Identity of a Home Assistant installation connected to a household. */
public final class HomeAssistantId extends Id<HomeAssistantConnection> {
    public HomeAssistantId(String value) { super(value, "homeassistant:"); }
}
