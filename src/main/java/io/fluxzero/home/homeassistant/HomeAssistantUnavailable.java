package io.fluxzero.home.homeassistant;

/** A product-facing integration failure that does not copy remote diagnostics or credentials into domain state. */
public class HomeAssistantUnavailable extends RuntimeException {
    public HomeAssistantUnavailable(String message) { super(message); }
}
