package io.fluxzero.home.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.math.BigDecimal;

/** A household moment or a sensor crossing, expressed in domain terms. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AutomationTrigger.HomeBecomes.class, name = "homeBecomes"),
    @JsonSubTypes.Type(value = AutomationTrigger.MeasurementCrosses.class, name = "measurementCrosses")
})
public sealed interface AutomationTrigger {
    /** Current source revision when a rule is defined or resumed; existing evidence is not a new trigger. */
    default long sourceRevision(io.fluxzero.sdk.modeling.Graph<Home> home) {
        return switch (this) {
            case HomeBecomes ignored -> home.revisionStateIndex();
            case MeasurementCrosses trigger -> home.find(new DeviceStatusId(trigger.deviceId().getFunctionalId()), DeviceStatus.class)
                    .map(io.fluxzero.sdk.modeling.Graph::revisionStateIndex).orElse(-1L);
        };
    }
    record HomeBecomes(HomeMode mode) implements AutomationTrigger {}
    record MeasurementCrosses(DeviceId deviceId, Measurement measurement, Direction direction,
                              BigDecimal threshold) implements AutomationTrigger {}
    enum Direction { RISES_ABOVE, FALLS_BELOW }
}
