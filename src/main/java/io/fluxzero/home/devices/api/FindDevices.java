package io.fluxzero.home.devices.api;

import io.fluxzero.home.devices.api.model.Capability;
import io.fluxzero.home.devices.api.model.Device;
import io.fluxzero.home.household.api.HomeId;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.tracking.handling.HandleQuery;
import io.fluxzero.sdk.tracking.handling.Request;
import java.util.List;

/**
 * Find devices within a known home, optionally filtered by capability.
 * The devices relationship supplies current component documents; no public device collection is required.
 * Results reflect committed search state, not an event-bound Graph or transactional membership read.
 */
public record FindDevices(HomeId homeId, Capability capability) implements Request<List<Device>> {
    @HandleQuery List<Device> handle() {
        var search = Fluxzero.search(Device.class).whereAncestor(homeId);
        if (capability != null) search = search.match(capability, "capabilities");
        return search.fetchAll();
    }
}
