package io.fluxzero.home.homeassistant.api.model;

import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.homeassistant.api.HomeAssistantId;
import io.fluxzero.sdk.modeling.Alias;
import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import io.fluxzero.sdk.modeling.Parent;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.With;

/** One chosen route for a Device; several sensor entities may together describe that device. */
@Model
@With
public record HomeAssistantDevice(@EntityId(prefix = "homeassistant-device:")
                                  @Parent(pathInParent = "homeAssistant") DeviceId deviceId,
                                  @Parent(pathInParent = "devices") HomeAssistantId connectionId,
                                  Set<String> entityIds, String problem) {
    @Alias(prefix = "homeassistant-entity:")
    public Set<String> entityAliases() {
        return entityIds.stream().map(id -> connectionId + "/" + id).collect(Collectors.toSet());
    }
}
