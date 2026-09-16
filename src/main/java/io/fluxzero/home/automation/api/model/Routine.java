package io.fluxzero.home.automation.api.model;

import io.fluxzero.home.automation.api.RoutineId;
import io.fluxzero.home.household.api.HomeId;
import io.fluxzero.home.scenes.api.SceneId;
import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import io.fluxzero.sdk.modeling.Parent;
import java.time.Instant;
import lombok.With;

/** Durable timing intent for a scene, with a generation that makes obsolete deliveries harmless. */
@Model
@With
public record Routine(@EntityId RoutineId routineId, @Parent(pathInParent = "routines") HomeId homeId,
                      RoutineDetails details, SceneId sceneId, RoutineTiming timing, boolean enabled,
                      Instant nextRun, long generation, String problem) {}
