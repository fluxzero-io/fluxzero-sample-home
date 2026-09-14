package io.fluxzero.home.homeassistant;

import io.fluxzero.home.model.HomeId;
import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import io.fluxzero.sdk.modeling.Parent;
import lombok.With;

import java.time.Duration;

/** An installation has its own lifecycle; deleting it also removes its bindings and owned refresh work. */
@Model
@With
public record HomeAssistantConnection(@EntityId HomeAssistantId connectionId,
                                      @Parent(pathInParent = "homeAssistants") HomeId homeId,
                                      HomeAssistantDetails details, Duration refreshInterval, String problem) {}
