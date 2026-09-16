package io.fluxzero.home.household.api;

import io.fluxzero.home.household.api.model.Home;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.tracking.handling.HandleQuery;
import io.fluxzero.sdk.tracking.handling.Request;

/** Load the authoritative home graph, including named relations and independent child histories. */
public record GetHome(HomeId homeId) implements Request<Graph<Home>> {
    @HandleQuery Graph<Home> handle() { return Fluxzero.loadGraph(homeId); }
}
