package io.fluxzero.home.command;

import io.fluxzero.home.model.Capability;
import io.fluxzero.home.model.Device;
import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.Measurement;
import io.fluxzero.home.model.Space;
import io.fluxzero.home.model.SpaceId;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import jakarta.annotation.Nullable;
import java.util.Map;
import java.util.Set;

import static io.fluxzero.home.model.Rules.named;
import static io.fluxzero.home.model.Rules.require;

/** Give a device a home and describe the things it can do and measure. */
public record AddDevice(DeviceId deviceId, SpaceId spaceId, String name, String label,
                        Set<Capability> capabilities, Set<Measurement> measurements) {
    public AddDevice { capabilities = Set.copyOf(capabilities); measurements = Set.copyOf(measurements); }
    @AssertLegal void validate(Space space) {
        named(name); require(!capabilities.isEmpty() || !measurements.isEmpty(), "A device must do or measure something.");
        require(label == null || !label.isBlank(), "A device label cannot be blank.");
    }
    @AssertLegal void requireNew(@Nullable Device existing) {
        require(existing == null, "This device already exists.");
    }
    @Apply Device apply(Space space) { return new Device(deviceId, spaceId, name, label, capabilities, measurements, Map.of()); }
}
