package io.fluxzero.home.command;

import io.fluxzero.home.model.Device;
import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.Space;
import io.fluxzero.home.model.SpaceId;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.persisting.eventsourcing.InterceptApply;
import jakarta.validation.constraints.NotNull;

import java.util.List;

import static io.fluxzero.home.model.Rules.require;

/** Move a device inside its home and clear its former room’s primary-light choice atomically. */
public record MoveDevice(DeviceId deviceId, @NotNull SpaceId destinationId) {
    @InterceptApply Object prepare(Graph<Device> device, Graph<Home> home) {
        if (device.get().spaceId().equals(destinationId)) return null;
        require(home.find(destinationId, Space.class).isPresent(),
                "Choose an existing space from this home.");
        return List.of(new ClearPrimaryLight(device.get().spaceId(), deviceId), this);
    }
    @Apply Device apply(Device device) { return device.withSpaceId(destinationId); }
}
