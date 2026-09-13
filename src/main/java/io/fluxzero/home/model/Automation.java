package io.fluxzero.home.model;

import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import io.fluxzero.sdk.modeling.Parent;
import java.time.Duration;
import java.time.Instant;
import lombok.With;

/** A scene that reacts to a meaningful change, with a cooldown and durable duplicate suppression. */
@Model
@With
public record Automation(@EntityId AutomationId automationId, @Parent(pathInParent = "automations") HomeId homeId,
                         String name, SceneId sceneId, AutomationTrigger trigger, Duration cooldown, boolean enabled,
                         Instant createdAt, long lastProcessedRevision, long executionCount, Instant lastExecutedAt, String problem) {}
