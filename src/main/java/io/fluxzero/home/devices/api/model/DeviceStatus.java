package io.fluxzero.home.devices.api.model;

import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import io.fluxzero.sdk.modeling.Parent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import lombok.With;

/** Device observations retain their own history for event-bound comparisons, separate from intentions. */
@Model
@With
public record DeviceStatus(@EntityId(prefix = "devicestatus:")
                           @Parent(pathInParent = "status") DeviceId deviceId,
                           Instant observedAt, Availability availability,
                           DeviceSettings reportedSettings,
                           Map<Measurement, BigDecimal> readings) {
    public DeviceStatus {
        readings = Map.copyOf(readings);
    }
}
