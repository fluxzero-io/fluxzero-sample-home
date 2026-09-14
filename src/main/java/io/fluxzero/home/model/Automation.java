package io.fluxzero.home.model;

import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import io.fluxzero.sdk.modeling.Parent;
import lombok.With;

import java.time.Duration;
import java.time.Instant;

/**
 * A scene that reacts to future changes, with a quiet interval between activations.
 *
 * @param effectiveFrom the start of the current definition, excluding earlier household changes
 * @param cooldownEndsAt the first instant at which another reaction is allowed; null before the first success
 */
@Model
@With
public record Automation(@EntityId AutomationId automationId, @Parent(pathInParent = "automations") HomeId homeId,
                         AutomationDetails details, SceneId sceneId, AutomationTrigger trigger, Duration cooldown, boolean enabled,
                         Instant effectiveFrom, Instant cooldownEndsAt, String problem) {}
