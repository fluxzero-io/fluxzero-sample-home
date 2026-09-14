package io.fluxzero.home.model;

import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import io.fluxzero.sdk.modeling.Parent;
import lombok.With;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** Device observations retain their own history for event-bound comparisons, separate from intentions. */
@Model
@With
public record DeviceStatus(@EntityId DeviceStatusId deviceStatusId,
                           @Parent(pathInParent = "status") DeviceId deviceId,
                           Instant observedAt, Availability availability,
                           DeviceSettings reportedSettings,
                           Map<Measurement, BigDecimal> readings) {
    public DeviceStatus {
        readings = Map.copyOf(readings);
    }
}
