package io.fluxzero.home.homeassistant;

import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.publishing.GatewayException;
import io.fluxzero.sdk.publishing.TimeoutException;
import io.fluxzero.sdk.tracking.handling.HandleCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import static io.fluxzero.home.homeassistant.HomeAssistantEndpoint.REQUEST_SETTINGS;
import static io.fluxzero.home.homeassistant.HomeAssistantEndpoint.requireSuccess;

/** Execute a service action; acknowledgement does not constitute a physical observation. */
public record CallHomeAssistantService(@NotNull HomeAssistantId connectionId, @NotNull @Valid HomeAssistantAction action) {
    @HandleCommand
    void handle() {
        var request = HomeAssistantEndpoint.load(connectionId)
                .post("api/services/" + action.domain() + "/" + action.service(), action.body());
        try {
            requireSuccess(Fluxzero.sendWebRequestAndWait(request, REQUEST_SETTINGS));
        } catch (GatewayException | TimeoutException failure) {
            throw new HomeAssistantUnavailable("Home Assistant could not be reached or did not respond in time.");
        }
    }
}
