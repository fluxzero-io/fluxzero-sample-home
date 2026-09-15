package io.fluxzero.home.homeassistant;

import io.fluxzero.sdk.common.exception.FunctionalException;

/** A product-facing integration failure that does not copy remote diagnostics or credentials into domain state. */
public class HomeAssistantUnavailable extends FunctionalException {
    public HomeAssistantUnavailable(String message) { super(message); }
}
