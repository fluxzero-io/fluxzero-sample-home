package io.fluxzero.home.devices.api;

import io.fluxzero.home.devices.api.model.Availability;
import io.fluxzero.home.devices.api.model.Device;
import io.fluxzero.home.devices.api.model.DeviceSettings;
import io.fluxzero.home.devices.api.model.DeviceStatus;
import io.fluxzero.home.devices.api.model.Measurement;
import io.fluxzero.sdk.common.Message;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.persisting.eventsourcing.InterceptApply;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** Record a complete observation; delayed or duplicate observations do not overwrite newer evidence. */
public record ReportDeviceStatus(@NotNull DeviceId deviceId,
                                 @NotNull Instant observedAt,
                                 @NotNull Availability availability, @NotNull @Valid DeviceSettings reportedSettings,
                                 @NotNull Map<@NotNull Measurement, @NotNull BigDecimal> readings) {
    @AssertTrue(message = "Report values within each measurement's range.")
    boolean hasValidReadings() {
        return readings.entrySet().stream().allMatch(entry -> entry.getKey().accepts(entry.getValue()));
    }

    @AssertLegal
    void wasObservedByPublication(Message message) {
        if (observedAt.isAfter(message.getTimestamp())) {
            throw new IllegalCommandException("An observation cannot come from the future.");
        }
    }

    @InterceptApply Object ignoreOld(@Nullable DeviceStatus status) {
        return status != null && observedAt != null && !observedAt.isAfter(status.observedAt()) ? null : this;
    }
    @AssertLegal void supportsReadings(Device device) {
        for (var kind : readings.keySet()) {
            if (!device.measurements().contains(kind)) {
                throw new IllegalCommandException("This device does not measure " + kind + ".");
            }
        }
    }
    @Apply Device confirmRequest(Device device) {
        if (availability != Availability.ONLINE || device.requestedAt() == null
                || !observedAt.isAfter(device.requestedAt())) return device;
        var pending = device.pendingSettings().withoutConfirmedBy(reportedSettings);
        return device.withPendingSettings(pending).withRequestedAt(pending.isEmpty() ? null : device.requestedAt());
    }

    @Apply DeviceStatus apply(@Nullable DeviceStatus status, Device device) {
        return new DeviceStatus(deviceId, observedAt, availability, reportedSettings, readings);
    }
}
