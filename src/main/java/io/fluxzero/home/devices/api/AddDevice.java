package io.fluxzero.home.devices.api;

import io.fluxzero.home.devices.api.model.Capability;
import io.fluxzero.home.devices.api.model.Device;
import io.fluxzero.home.devices.api.model.DeviceDetails;
import io.fluxzero.home.devices.api.model.DeviceSettings;
import io.fluxzero.home.devices.api.model.Measurement;
import io.fluxzero.home.household.api.SpaceId;
import io.fluxzero.home.household.api.model.Space;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

/** Give a device a home and describe the things it can do and measure. */
public record AddDevice(DeviceId deviceId, SpaceId spaceId, @NotNull @Valid DeviceDetails details, String label,
                        @NotNull Set<@NotNull Capability> capabilities, @NotNull Set<@NotNull Measurement> measurements) {
    @AssertTrue(message = "A device must do or measure something.")
    boolean hasCapabilitiesOrMeasurements() {
        return !capabilities.isEmpty() || !measurements.isEmpty();
    }

    @AssertTrue(message = "A device label cannot be blank.")
    boolean hasValidLabel() {
        return label == null || !label.isBlank();
    }

    @Apply Device apply(Space space) { return new Device(deviceId, spaceId, details, label, capabilities, measurements, DeviceSettings.empty(), null); }
}
