package io.fluxzero.home.scenes.api;

import io.fluxzero.home.scenes.api.model.Scene;
import io.fluxzero.sdk.modeling.Id;

/** Stable identity of a scene; its prefix prevents collisions with other kinds of model. */
public final class SceneId extends Id<Scene> {
    public SceneId(String value) { super(value, "scene:"); }
}
