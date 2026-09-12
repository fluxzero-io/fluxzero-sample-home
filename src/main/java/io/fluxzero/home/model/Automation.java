package io.fluxzero.home.model;

import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import io.fluxzero.sdk.modeling.Parent;
import java.time.Duration;
import java.time.Instant;
import lombok.With;

import static io.fluxzero.sdk.modeling.ModelPersistence.DOCUMENT;
import static io.fluxzero.sdk.modeling.ModelPersistence.EVENT_SOURCED;

/** A scene that reacts to a meaningful change, with a cooldown and durable duplicate suppression. */
@Model(persistence = {EVENT_SOURCED, DOCUMENT})
@With
public record Automation(@EntityId AutomationId automationId, @Parent(pathInParent = "automations") HomeId homeId,
                         String name, SceneId sceneId, AutomationTrigger trigger, Duration cooldown, boolean enabled,
                         Instant createdAt, long lastProcessedRevision, long executionCount, Instant lastExecutedAt, String problem) {}
