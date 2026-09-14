package io.fluxzero.home.automation;

import io.fluxzero.home.model.AutomationTrigger;
import io.fluxzero.home.model.DeviceStatusId;
import io.fluxzero.home.model.HomeMode;
import io.fluxzero.home.model.Measurement;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** Internal evidence of a committed change; it never pretends that a requested device setting was observed. */
public record HomeSignal(DeviceStatusId statusId, Instant at, HomeMode beforeMode, HomeMode mode,
                         Map<Measurement, BigDecimal> before, Map<Measurement, BigDecimal> readings) {
    public HomeSignal { before = Map.copyOf(before); readings = Map.copyOf(readings); }
    public boolean matches(AutomationTrigger trigger) {
        return switch (trigger) {
            case AutomationTrigger.HomeBecomes t -> statusId == null && mode == t.mode() && beforeMode != mode;
            case AutomationTrigger.MeasurementCrosses t -> {
                if (statusId == null || !statusId.equals(new DeviceStatusId(t.deviceId().getFunctionalId()))) yield false;
                var previous = before.get(t.measurement()); var current = readings.get(t.measurement());
                if (previous == null || current == null) yield false;
                yield t.direction() == AutomationTrigger.Direction.RISES_ABOVE
                        ? previous.compareTo(t.threshold()) <= 0 && current.compareTo(t.threshold()) > 0
                        : previous.compareTo(t.threshold()) >= 0 && current.compareTo(t.threshold()) < 0;
            }
        };
    }
}
