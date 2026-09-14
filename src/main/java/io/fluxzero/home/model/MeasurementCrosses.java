package io.fluxzero.home.model;

import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** React when two readings from the chosen device prove a threshold crossing. */
public record MeasurementCrosses(@NotNull DeviceId deviceId, @NotNull Measurement measurement,
                                 @NotNull Direction direction, @NotNull BigDecimal threshold)
        implements AutomationTrigger {
    public enum Direction { RISES_ABOVE, FALLS_BELOW }

    @AssertTrue(message = "Choose a threshold within the measurement's range.")
    boolean hasValidThreshold() {
        return measurement.accepts(threshold);
    }

    @AssertLegal
    void deviceSuppliesMeasurement(Graph<Home> home) {
        var device = home.find(deviceId, Device.class)
                .orElseThrow(() -> new IllegalCommandException("Choose an existing device from this home.")).get();
        if (!device.measurements().contains(measurement)) {
            throw new IllegalCommandException("Choose a measurement supplied by this device.");
        }
    }

    @Override
    public boolean matches(HomeChange change) {
        if (!(change instanceof DeviceObservationChanged c) || !deviceId.equals(c.deviceId())) {
            return false;
        }
        var before = c.before().get(measurement);
        var after = c.after().get(measurement);
        if (before == null || after == null) {
            return false;
        }
        return switch (direction) {
            case RISES_ABOVE -> before.compareTo(threshold) <= 0 && after.compareTo(threshold) > 0;
            case FALLS_BELOW -> before.compareTo(threshold) >= 0 && after.compareTo(threshold) < 0;
        };
    }
}
