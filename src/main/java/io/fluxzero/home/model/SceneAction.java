package io.fluxzero.home.model;

/** One part of a scene; its order matters when later actions refine earlier settings. */
public record SceneAction(SceneTarget target, DeviceSetting setting) {
    public SceneAction { java.util.Objects.requireNonNull(target); java.util.Objects.requireNonNull(setting); }
}
