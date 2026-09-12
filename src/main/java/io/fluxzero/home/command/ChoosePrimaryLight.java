package io.fluxzero.home.command;

import io.fluxzero.home.model.Capability;
import io.fluxzero.home.model.Device;
import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.Space;
import io.fluxzero.home.model.SpaceId;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

import static io.fluxzero.home.model.Rules.require;

/** Select the light that represents a room’s everyday lighting. */
public record ChoosePrimaryLight(SpaceId spaceId, DeviceId deviceId) {
    @AssertLegal void validate(Space space, Device device) {
        require(device.spaceId().equals(spaceId), "Choose a light in this space.");
        require(device.capabilities().contains(Capability.LIGHT_LEVEL), "Choose a dimmable light.");
    }
    @Apply Space apply(Space space) { return space.withPrimaryLightId(deviceId); }
}
