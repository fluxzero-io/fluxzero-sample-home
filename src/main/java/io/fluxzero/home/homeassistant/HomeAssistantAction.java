package io.fluxzero.home.homeassistant;

import com.fasterxml.jackson.annotation.JsonProperty;

/** An adapter-local service call with an explicit wire body. Not a domain command. */
public record HomeAssistantAction(String domain, String service, Body body) {
    public sealed interface Body permits SwitchEntity, DimEntity {}

    public record SwitchEntity(@JsonProperty("entity_id") String entityId) implements Body {}
    public record DimEntity(@JsonProperty("entity_id") String entityId,
                            @JsonProperty("brightness_pct") int brightnessPercent) implements Body {}
}
