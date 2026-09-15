package io.fluxzero.home.homeassistant;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Service details carried by CallHomeAssistantService, with an explicit REST body. */
public record HomeAssistantAction(String domain, String service, Body body) {
    public sealed interface Body permits SwitchEntity, DimEntity {}

    public record SwitchEntity(@JsonProperty("entity_id") String entityId) implements Body {}
    public record DimEntity(@JsonProperty("entity_id") String entityId,
                            @JsonProperty("brightness_pct") int brightnessPercent) implements Body {}
}
