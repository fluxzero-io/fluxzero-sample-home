package io.fluxzero.home.homeassistant;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/** Service details carried by CallHomeAssistantService, with an explicit REST body. */
public record HomeAssistantAction(String domain, String service,
                                  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
                                  @JsonSubTypes({@JsonSubTypes.Type(value = SwitchEntity.class, name = "switch"),
                                          @JsonSubTypes.Type(value = DimEntity.class, name = "dim")}) Body body) {
    public sealed interface Body permits SwitchEntity, DimEntity {}

    public record SwitchEntity(@JsonProperty("entity_id") String entityId) implements Body {}
    public record DimEntity(@JsonProperty("entity_id") String entityId,
                            @JsonProperty("brightness_pct") int brightnessPercent) implements Body {}
}
