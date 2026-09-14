package io.fluxzero.home.homeassistant;

import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.tracking.handling.HandleQuery;
import io.fluxzero.sdk.tracking.handling.Request;

/** Inspect the connection, chosen device routes and current integration problems. */
public record GetHomeAssistant(HomeAssistantId connectionId) implements Request<Graph<HomeAssistantConnection>> {
    @HandleQuery Graph<HomeAssistantConnection> handle() { return Fluxzero.loadGraph(connectionId); }
}
