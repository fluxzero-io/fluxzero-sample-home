package io.fluxzero.home.model;

import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import io.fluxzero.sdk.modeling.Parent;
import java.time.Instant;
import java.util.Map;
import lombok.With;

import static io.fluxzero.sdk.modeling.ModelPersistence.DOCUMENT;

/** The latest complete device report, separate from intentions and their event histories. */
@Model(persistence = DOCUMENT)
@With
public record DeviceStatus(@EntityId DeviceStatusId deviceStatusId,
                           @Parent(pathInParent = "status") DeviceId deviceId,
                           Instant observedAt, Availability availability,
                           Map<Capability, DeviceSetting> reportedSettings,
                           Map<Measurement, java.math.BigDecimal> readings,
                           Map<Measurement, java.math.BigDecimal> previousReadings) {
    public DeviceStatus {
        reportedSettings = Map.copyOf(reportedSettings);
        readings = Map.copyOf(readings);
        previousReadings = Map.copyOf(previousReadings);
    }
}
