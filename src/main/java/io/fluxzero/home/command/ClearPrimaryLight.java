package io.fluxzero.home.command;

import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.Space;
import io.fluxzero.home.model.SpaceId;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Clear a room preference only when it still refers to the named light. */
public record ClearPrimaryLight(SpaceId spaceId, DeviceId deviceId) {
    @Apply Space apply(Space space) {
        return deviceId.equals(space.primaryLightId()) ? space.withPrimaryLightId(null) : space;
    }
}
