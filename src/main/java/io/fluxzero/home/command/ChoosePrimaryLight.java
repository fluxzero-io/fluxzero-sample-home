package io.fluxzero.home.command;

import io.fluxzero.home.model.Capability;
import io.fluxzero.home.model.Device;
import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.Space;
import io.fluxzero.home.model.SpaceId;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;

/** Select the light that represents a room’s everyday lighting. */
public record ChoosePrimaryLight(SpaceId spaceId, DeviceId deviceId) {
    @AssertLegal void validate(Space space, Device device) {
        if (!device.spaceId().equals(spaceId)) {
            throw new IllegalCommandException("Choose a light in this space.");
        }
        if (!device.capabilities().contains(Capability.LIGHT_LEVEL)) {
            throw new IllegalCommandException("Choose a dimmable light.");
        }
    }
    @Apply Space apply(Space space) { return space.withPrimaryLightId(deviceId); }
}
