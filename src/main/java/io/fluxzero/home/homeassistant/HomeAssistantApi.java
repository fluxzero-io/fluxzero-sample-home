package io.fluxzero.home.homeassistant;

import com.fasterxml.jackson.databind.JsonNode;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.publishing.GatewayException;
import io.fluxzero.sdk.publishing.TimeoutException;
import io.fluxzero.sdk.web.RedirectPolicy;
import io.fluxzero.sdk.web.WebRequest;
import io.fluxzero.sdk.web.WebRequestSettings;
import io.fluxzero.sdk.web.WebResponse;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;

/** Home Assistant's REST contract over the auditable Fluxzero web gateway. */
@Component
public class HomeAssistantApi {
    private static final WebRequestSettings REQUEST_SETTINGS = WebRequestSettings.builder()
            .timeout(Duration.ofSeconds(5)).redirectPolicy(RedirectPolicy.NEVER)
            .maxRetries(2).retryDelay(Duration.ofMillis(250)).build();

    public HomeAssistantSnapshot states(HomeAssistantConnection connection) {
        var response = exchange(connection, "api/states", null);
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

    public void call(HomeAssistantConnection connection, HomeAssistantAction action) {
        exchange(connection, "api/services/" + action.domain() + "/" + action.service(), action.body());
        // HTTP success confirms the service call, not the resulting physical state.
    }

    private WebResponse exchange(HomeAssistantConnection connection, String path, Object body) {
        var access = HomeAssistantAccess.load(connection);
        var url = access.baseUrl.resolve(path).toString();
        var request = (body == null ? WebRequest.get(url)
                : WebRequest.post(url).contentType("application/json").body(body))
                .header("Authorization", "Bearer " + access.token).header("Accept", "application/json").build();
        WebResponse response;
        try {
            response = Fluxzero.sendWebRequestAndWait(request, REQUEST_SETTINGS);
        } catch (GatewayException | TimeoutException failure) {
            throw new HomeAssistantUnavailable("Home Assistant could not be reached or did not respond in time.");
        }
        if (response.getStatus() == 401 || response.getStatus() == 403) {
            throw new HomeAssistantUnavailable("Home Assistant refused access. Check the configured token and permissions.");
        }
        if (response.getStatus() < 200 || response.getStatus() >= 300) {
            throw new HomeAssistantUnavailable("Home Assistant returned HTTP " + response.getStatus() + ".");
        }
        return response;
    }
}
