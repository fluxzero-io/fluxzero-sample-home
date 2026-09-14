package io.fluxzero.home.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** A household moment or a sensor crossing, expressed in domain terms. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AutomationTrigger.HomeBecomes.class, name = "homeBecomes"),
    @JsonSubTypes.Type(value = AutomationTrigger.MeasurementCrosses.class, name = "measurementCrosses")
})
public sealed interface AutomationTrigger {
    record HomeBecomes(@NotNull HomeMode mode) implements AutomationTrigger {}
    record MeasurementCrosses(@NotNull DeviceId deviceId, @NotNull Measurement measurement, @NotNull Direction direction,
                              @NotNull BigDecimal threshold) implements AutomationTrigger {
        @AssertTrue(message = "Choose a threshold within the measurement's range.")
        boolean hasValidThreshold() {
            return measurement == null || threshold == null || measurement.accepts(threshold);
        }
    }
    enum Direction { RISES_ABOVE, FALLS_BELOW }
}
