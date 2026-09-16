package io.fluxzero.home.household.api;

import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.devices.api.model.Capability;
import io.fluxzero.home.devices.api.model.Device;
import io.fluxzero.home.household.api.model.Space;
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
