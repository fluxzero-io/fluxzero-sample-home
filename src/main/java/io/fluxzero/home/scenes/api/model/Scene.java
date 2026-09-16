package io.fluxzero.home.scenes.api.model;

import io.fluxzero.home.household.api.HomeId;
import io.fluxzero.home.scenes.api.SceneId;
import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import io.fluxzero.sdk.modeling.Parent;
import java.util.List;
import lombok.With;

/** A reusable intention such as good morning, dinner or everything off. */
@Model
@With
public record Scene(@EntityId SceneId sceneId, @Parent(pathInParent = "scenes") HomeId homeId,
                    SceneDetails details, List<SceneAction> actions) {
    public Scene { actions = List.copyOf(actions); }
}
