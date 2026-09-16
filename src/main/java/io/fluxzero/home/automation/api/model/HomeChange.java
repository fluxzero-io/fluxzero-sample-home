package io.fluxzero.home.automation.api.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Instant;

/** Evidence of a committed household change at the original event boundary. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = HomeModeChanged.class, name = "homeModeChanged"),
    @JsonSubTypes.Type(value = DeviceObservationChanged.class, name = "deviceObservationChanged")
})
public sealed interface HomeChange permits HomeModeChanged, DeviceObservationChanged {
    Instant at();
}
