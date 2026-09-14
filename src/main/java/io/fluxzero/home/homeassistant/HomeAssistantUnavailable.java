package io.fluxzero.home.homeassistant;

/** A sanitized integration failure: remote response bodies, request headers and credentials stay outside messages. */
public class HomeAssistantUnavailable extends RuntimeException {
    public HomeAssistantUnavailable(String message) { super(message); }
}
