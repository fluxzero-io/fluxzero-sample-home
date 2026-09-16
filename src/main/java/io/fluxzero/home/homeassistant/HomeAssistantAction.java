package io.fluxzero.home.homeassistant;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

/** Service details carried by CallHomeAssistantService, with an explicit REST body. */
public record HomeAssistantAction(String domain, String service, Body body) {
    public sealed interface Body permits SwitchEntity, DimEntity, ColorEntity, SetTemperature, SetPosition {}

    public record SwitchEntity(@JsonProperty("entity_id") String entityId) implements Body {}
    public record DimEntity(@JsonProperty("entity_id") String entityId,
                            @JsonProperty("brightness_pct") int brightnessPercent) implements Body {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ColorEntity(@JsonProperty("entity_id") String entityId,
                              @JsonProperty("hs_color") List<Integer> hueSaturation,
                              @JsonProperty("brightness_pct") Integer brightnessPercent) implements Body {}

    public record SetTemperature(@JsonProperty("entity_id") String entityId, BigDecimal temperature) implements Body {}

    public record SetPosition(@JsonProperty("entity_id") String entityId, int position) implements Body {}
}
