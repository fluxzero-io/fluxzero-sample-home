package io.fluxzero.home.query;

import io.fluxzero.home.model.Capability;
import io.fluxzero.home.model.Device;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.tracking.handling.HandleQuery;
import io.fluxzero.sdk.tracking.handling.Request;
import java.util.List;

/** Find devices in a home by what they can do, using current documents and durable ancestry. */
public record FindDevices(HomeId homeId, Capability capability) implements Request<List<Device>> {
    @HandleQuery List<Device> handle() {
        var search = Fluxzero.search(Device.class).whereAncestor(homeId);
        if (capability != null) search = search.match(capability, "capabilities");
        return search.fetchAll();
    }
}
