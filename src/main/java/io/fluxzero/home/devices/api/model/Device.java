package io.fluxzero.home.devices.api.model;

import io.fluxzero.home.devices.api.DeviceCommand;
import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.devices.api.ReportDeviceStatus;
import io.fluxzero.home.household.api.SpaceId;
import io.fluxzero.sdk.modeling.Alias;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import io.fluxzero.sdk.modeling.Parent;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;
import java.util.Set;
import lombok.With;

/** A named device and the desired settings requested by the household. */
@Model
@With
public record Device(@EntityId DeviceId deviceId,
                     @Parent(pathInParent = "devices") SpaceId spaceId,
                     DeviceDetails details, @Alias(prefix = "device-label:") String label,
                     Set<Capability> capabilities, Set<Measurement> measurements,
                     DeviceSettings desiredSettings) {
    public Device {
        capabilities = Set.copyOf(capabilities);
        measurements = Set.copyOf(measurements);
    }

    @AssertLegal
    void supportsRequestedSetting(DeviceCommand command) {
        requireCapability(command.setting().capability());
    }

    @AssertLegal
    void supportsReportedSettings(ReportDeviceStatus report) {
        report.reportedSettings().values().forEach(setting -> requireCapability(setting.capability()));
    }

    private void requireCapability(Capability capability) {
        if (!capabilities.contains(capability)) {
            throw new IllegalCommandException(details.name() + " does not support " + capability + ".");
        }
    }
}
