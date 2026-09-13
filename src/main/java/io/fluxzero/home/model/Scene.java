package io.fluxzero.home.model;

import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import io.fluxzero.sdk.modeling.Parent;
import java.time.Instant;
import java.util.List;
import lombok.With;

/** A reusable intention such as good morning, dinner or everything off. */
@Model
@With
public record Scene(@EntityId SceneId sceneId, @Parent(pathInParent = "scenes") HomeId homeId,
                    SceneDetails details, List<SceneAction> actions, long activationCount, Instant lastActivatedAt) {
    public Scene { actions = List.copyOf(actions); }
}
