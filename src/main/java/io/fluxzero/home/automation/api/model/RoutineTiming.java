package io.fluxzero.home.automation.api.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.time.ZoneId;

/** A one-off moment or a weekly local-time rhythm, independent of machine time. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Once.class, name = "once"),
    @JsonSubTypes.Type(value = Weekly.class, name = "weekly")
})
public sealed interface RoutineTiming permits Once, Weekly {
    /** The next occurrence strictly after the given moment, or null when the pattern has ended. */
    @Nullable
    Instant nextAfter(Instant after, ZoneId zone);
}
