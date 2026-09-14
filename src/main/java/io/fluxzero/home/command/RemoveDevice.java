package io.fluxzero.home.command;

import io.fluxzero.home.model.Automation;
import io.fluxzero.home.model.Device;
import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.MeasurementCrosses;
import io.fluxzero.home.model.OneDevice;
import io.fluxzero.home.model.Scene;
import io.fluxzero.home.model.Space;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.tracking.handling.Association;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;

/** Remove a device and clear its room’s primary-light choice in one RC10 parent/child commit. */
public record RemoveDevice(DeviceId deviceId) {
    @AssertLegal void validate(Device device, Graph<Home> home) {
        if (home.childModels(Scene.class).stream().flatMap(s -> s.actions().stream())
                .anyMatch(a -> a.target() instanceof OneDevice one && one.deviceId().equals(deviceId))) {
            throw new IllegalCommandException("Remove this device from its scenes first.");
        }
        if (home.childModels(Automation.class).stream().anyMatch(a -> a.trigger() instanceof MeasurementCrosses m
                && m.deviceId().equals(deviceId))) {
            throw new IllegalCommandException("Remove this device from its automations first.");
        }
    }
    @Apply Device remove(Device device) { return null; }
    @Apply Space clearPrimaryLight(@Association("devices") Space space) {
        return deviceId.equals(space.primaryLightId()) ? space.withPrimaryLightId(null) : space;
    }
}
