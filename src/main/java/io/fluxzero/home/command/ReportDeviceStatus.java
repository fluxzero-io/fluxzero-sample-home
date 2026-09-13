package io.fluxzero.home.command;

import io.fluxzero.home.model.Availability;
import io.fluxzero.home.model.Capability;
import io.fluxzero.home.model.Device;
import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.DeviceSetting;
import io.fluxzero.home.model.DeviceStatus;
import io.fluxzero.home.model.DeviceStatusId;
import io.fluxzero.home.model.Measurement;
import io.fluxzero.sdk.common.Message;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.persisting.eventsourcing.InterceptApply;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.Map;

import static io.fluxzero.home.model.Rules.require;

/** Record a complete observation; delayed or duplicate observations do not overwrite newer evidence. */
public record ReportDeviceStatus(DeviceStatusId deviceStatusId, DeviceId deviceId, Instant observedAt,
                                 Availability availability, Map<Capability, DeviceSetting> reportedSettings,
                                 Map<Measurement, java.math.BigDecimal> readings) {
    public ReportDeviceStatus { reportedSettings = Map.copyOf(reportedSettings); readings = Map.copyOf(readings); }
    @InterceptApply Object ignoreOld(@Nullable DeviceStatus status) {
        return status != null && observedAt != null && !observedAt.isAfter(status.observedAt()) ? null : this;
    }
    @AssertLegal void validate(Device device, Message message) {
        require(deviceStatusId.equals(new DeviceStatusId(deviceId.getFunctionalId())), "Use this device’s status identity.");
        require(observedAt != null && !observedAt.isAfter(message.getTimestamp()), "An observation cannot come from the future.");
        require(availability != null, "Report whether the device is reachable.");
        reportedSettings.forEach((key,value) -> { require(key == value.capability(), "The reported setting has the wrong kind."); device.assertSupports(value); });
        readings.forEach((kind,value) -> { require(device.measurements().contains(kind), "This device does not measure " + kind + "."); kind.validate(value); });
    }
    @Apply DeviceStatus apply(@Nullable DeviceStatus status, Device device) {
        return new DeviceStatus(deviceStatusId, deviceId, observedAt, availability, reportedSettings, readings);
    }
}
