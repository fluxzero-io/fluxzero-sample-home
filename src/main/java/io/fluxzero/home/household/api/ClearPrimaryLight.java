package io.fluxzero.home.household.api;

import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.household.api.model.Space;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Clear a room preference only when it still refers to the named light. */
public record ClearPrimaryLight(SpaceId spaceId, DeviceId deviceId) {
    @Apply Space apply(Space space) {
        return deviceId.equals(space.primaryLightId()) ? space.withPrimaryLightId(null) : space;
    }
}
