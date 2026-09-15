package io.fluxzero.home.homeassistant;

import com.fasterxml.jackson.databind.JsonNode;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.publishing.GatewayException;
import io.fluxzero.sdk.publishing.TimeoutException;
import io.fluxzero.sdk.tracking.Consumer;
import io.fluxzero.sdk.tracking.TrackSelf;
import io.fluxzero.sdk.tracking.handling.HandleQuery;
import io.fluxzero.sdk.tracking.handling.Request;
import io.fluxzero.sdk.web.WebResponse;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;

import static io.fluxzero.home.homeassistant.HomeAssistantEndpoint.REQUEST_SETTINGS;
import static io.fluxzero.home.homeassistant.HomeAssistantEndpoint.requireSuccess;

/** Read one complete REST snapshot from an operator-configured Home Assistant installation. */
@TrackSelf
@Consumer(name = "home-assistant-api", singleTracker = true)
public record GetHomeAssistantStates(@NotNull HomeAssistantId connectionId) implements Request<HomeAssistantSnapshot> {
    @HandleQuery
    HomeAssistantSnapshot handle() {
        var request = HomeAssistantEndpoint.load(connectionId).get("api/states");
        WebResponse response;
        try {
            response = requireSuccess(Fluxzero.sendWebRequestAndWait(request, REQUEST_SETTINGS));
        } catch (GatewayException | TimeoutException failure) {
            throw new HomeAssistantUnavailable("Home Assistant could not be reached or did not respond in time.");
        }
        try {
            var states = response.<JsonNode>getPayloadAs(JsonNode.class);
            if (states == null || !states.isArray()) throw new IllegalArgumentException("Expected a state array");
            var result = new ArrayList<HomeAssistantState>();
            for (var state : states) result.add(HomeAssistantState.from(state));
            return new HomeAssistantSnapshot(result);
        } catch (Exception invalid) {
            throw new HomeAssistantUnavailable("Home Assistant returned an invalid state snapshot.");
        }
    }
}
