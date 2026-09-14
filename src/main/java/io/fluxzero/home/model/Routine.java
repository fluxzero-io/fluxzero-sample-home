package io.fluxzero.home.model;

import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import io.fluxzero.sdk.modeling.Parent;
import lombok.With;

import java.time.Instant;

/** Durable timing intent for a scene, with a generation that makes obsolete deliveries harmless. */
@Model
@With
public record Routine(@EntityId RoutineId routineId, @Parent(pathInParent = "routines") HomeId homeId,
                      RoutineDetails details, SceneId sceneId, RoutineTiming timing, boolean enabled,
                      Instant nextRun, long generation, String problem) {}
