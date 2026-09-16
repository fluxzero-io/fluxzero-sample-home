package io.fluxzero.home.homeassistant.api.model;

import io.fluxzero.home.homeassistant.api.HomeAssistantId;
import io.fluxzero.home.household.api.HomeId;
import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import io.fluxzero.sdk.modeling.Parent;
import java.time.Duration;
import lombok.With;

/** An installation has its own lifecycle; deleting it also removes its bindings and owned refresh work. */
@Model
@With
public record HomeAssistantConnection(@EntityId HomeAssistantId connectionId,
                                      @Parent(pathInParent = "homeAssistants") HomeId homeId,
                                      HomeAssistantDetails details, Duration refreshInterval, String problem) {}
