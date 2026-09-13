package io.fluxzero.home.model;

import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import io.fluxzero.sdk.modeling.Parent;
import java.time.Instant;
import lombok.With;

/** Durable timing intent for a scene, with a generation that makes obsolete deliveries harmless. */
@Model
@With
public record Routine(@EntityId RoutineId routineId, @Parent(pathInParent = "routines") HomeId homeId,
                      String name, SceneId sceneId, RoutineTiming timing, boolean enabled,
                      Instant nextRun, long generation, long executionCount, Instant lastExecutedAt, String problem) {}
