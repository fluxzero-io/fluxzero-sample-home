package io.fluxzero.home.command;

import io.fluxzero.home.model.Availability;
import io.fluxzero.home.model.Device;
import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.DeviceSettings;
import io.fluxzero.home.model.DeviceStatus;
import io.fluxzero.home.model.DeviceStatusId;
import io.fluxzero.home.model.Measurement;
import io.fluxzero.sdk.common.Message;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.persisting.eventsourcing.InterceptApply;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static io.fluxzero.home.model.Rules.require;

/** Record a complete observation; delayed or duplicate observations do not overwrite newer evidence. */
public record ReportDeviceStatus(@NotNull DeviceStatusId deviceStatusId, @NotNull DeviceId deviceId,
                                 @NotNull Instant observedAt,
                                 @NotNull Availability availability, @NotNull @Valid DeviceSettings reportedSettings,
                                 @NotNull Map<@NotNull Measurement, @NotNull BigDecimal> readings) {
    @AssertTrue(message = "Use this device’s status identity.")
    boolean hasMatchingIdentity() {
        return deviceStatusId.equals(new DeviceStatusId(deviceId.getFunctionalId()));
    }

    @AssertTrue(message = "Report values within each measurement's range.")
    boolean hasValidReadings() {
        return readings.entrySet().stream().allMatch(entry -> entry.getKey().accepts(entry.getValue()));
    }

    @AssertLegal
    void wasObservedByPublication(Message message) {
        require(!observedAt.isAfter(message.getTimestamp()), "An observation cannot come from the future.");
    }

    @InterceptApply Object ignoreOld(@Nullable DeviceStatus status) {
        return status != null && observedAt != null && !observedAt.isAfter(status.observedAt()) ? null : this;
    }
    @AssertLegal void supportsReadings(Device device) {
        readings.keySet().forEach(kind -> require(device.measurements().contains(kind),
                "This device does not measure " + kind + "."));
    }
    @Apply DeviceStatus apply(@Nullable DeviceStatus status, Device device) {
        return new DeviceStatus(deviceStatusId, deviceId, observedAt, availability, reportedSettings, readings);
    }
}
