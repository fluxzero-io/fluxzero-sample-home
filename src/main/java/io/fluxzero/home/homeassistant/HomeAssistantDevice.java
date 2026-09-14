package io.fluxzero.home.homeassistant;

import io.fluxzero.home.model.DeviceId;
import io.fluxzero.sdk.modeling.Alias;
import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import io.fluxzero.sdk.modeling.Parent;
import lombok.With;

import java.util.Set;
import java.util.stream.Collectors;

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
