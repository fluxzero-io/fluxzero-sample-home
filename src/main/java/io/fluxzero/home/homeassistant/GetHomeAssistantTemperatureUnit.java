package io.fluxzero.home.homeassistant;

import com.fasterxml.jackson.databind.JsonNode;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.publishing.GatewayException;
import io.fluxzero.sdk.publishing.TimeoutException;
import io.fluxzero.sdk.tracking.handling.HandleQuery;
import io.fluxzero.sdk.tracking.handling.Request;
import io.fluxzero.sdk.web.WebResponse;
import jakarta.validation.constraints.NotNull;

import static io.fluxzero.home.homeassistant.HomeAssistantEndpoint.REQUEST_SETTINGS;
import static io.fluxzero.home.homeassistant.HomeAssistantEndpoint.requireSuccess;

/** Climate REST attributes and service temperatures use the installation's configured temperature unit. */
public record GetHomeAssistantTemperatureUnit(@NotNull HomeAssistantId connectionId) implements Request<String> {
    @HandleQuery
    String handle() {
        var request = HomeAssistantEndpoint.load(connectionId).get("api/config");
        WebResponse response;
        try {
            response = requireSuccess(Fluxzero.sendWebRequestAndWait(request, REQUEST_SETTINGS));
        } catch (GatewayException | TimeoutException failure) {
            throw new HomeAssistantUnavailable("Home Assistant could not be reached or did not respond in time.");
        }
        try {
            String unit = response.<JsonNode>getPayloadAs(JsonNode.class).path("unit_system").path("temperature").asText();
            if (unit.equals("°C") || unit.equals("°F")) return unit;
        } catch (Exception invalid) {
            // Report a configuration problem without copying remote content into the error.
        }
        throw new HomeAssistantUnavailable("Home Assistant did not report a supported temperature unit.");
    }
}
