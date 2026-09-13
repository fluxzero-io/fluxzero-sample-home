package io.fluxzero.home.model;

import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import io.fluxzero.sdk.modeling.Parent;
import java.time.Instant;
import java.util.Map;
import lombok.With;

/** Device observations retain their own history for event-bound comparisons, separate from intentions. */
@Model
@With
public record DeviceStatus(@EntityId DeviceStatusId deviceStatusId,
                           @Parent(pathInParent = "status") DeviceId deviceId,
                           Instant observedAt, Availability availability,
                           Map<Capability, DeviceSetting> reportedSettings,
                           Map<Measurement, java.math.BigDecimal> readings) {
    public DeviceStatus {
        reportedSettings = Map.copyOf(reportedSettings);
        readings = Map.copyOf(readings);
    }
}
