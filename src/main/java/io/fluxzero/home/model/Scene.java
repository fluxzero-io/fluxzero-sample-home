package io.fluxzero.home.model;

import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import io.fluxzero.sdk.modeling.Parent;
import java.time.Instant;
import java.util.List;
import lombok.With;

import static io.fluxzero.sdk.modeling.ModelPersistence.DOCUMENT;
import static io.fluxzero.sdk.modeling.ModelPersistence.EVENT_SOURCED;

/** A reusable intention such as good morning, dinner or everything off. */
@Model(persistence = {EVENT_SOURCED, DOCUMENT})
@With
public record Scene(@EntityId SceneId sceneId, @Parent(pathInParent = "scenes") HomeId homeId,
                    String name, List<SceneAction> actions, long activationCount, Instant lastActivatedAt) {
    public Scene { actions = List.copyOf(actions); }
}
