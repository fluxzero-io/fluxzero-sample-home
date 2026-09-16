package io.fluxzero.home.automation.api.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/** A household moment or a sensor crossing, expressed in domain terms. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = HomeBecomes.class, name = "homeBecomes"),
    @JsonSubTypes.Type(value = MeasurementCrosses.class, name = "measurementCrosses")
})
public sealed interface AutomationTrigger permits HomeBecomes, MeasurementCrosses {
    boolean matches(HomeChange change);
}
