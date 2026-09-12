package io.fluxzero.home.command;

import io.fluxzero.home.model.Device;
import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.ScenePlan;
import io.fluxzero.home.model.Space;
import io.fluxzero.home.model.SpaceId;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.persisting.eventsourcing.InterceptApply;
import java.util.List;

/** Move a device inside its home and clear its former room’s primary-light choice atomically. */
public record MoveDevice(DeviceId deviceId, SpaceId destinationId) {
    @InterceptApply Object prepare(Graph<Device> device, Graph<Home> home) {
        if (device.get().spaceId().equals(destinationId)) return null;
        ScenePlan.find(home, destinationId, Space.class);
        return List.of(new ClearPrimaryLight(device.get().spaceId(), deviceId), this);
    }
    @Apply Device apply(Device device) { return device.withSpaceId(destinationId); }
}
