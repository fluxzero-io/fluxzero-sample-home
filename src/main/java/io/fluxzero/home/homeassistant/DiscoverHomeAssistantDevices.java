package io.fluxzero.home.homeassistant;

import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.tracking.handling.HandleQuery;
import io.fluxzero.sdk.tracking.handling.Request;

import java.util.List;

/** Preview supported entities and their capabilities without creating household devices. */
public record DiscoverHomeAssistantDevices(HomeAssistantId connectionId) implements Request<List<HomeAssistantEntity>> {
    @HandleQuery
    List<HomeAssistantEntity> handle() {
        return Fluxzero.queryAndWait(new GetHomeAssistantStates(connectionId)).discover();
    }
}
