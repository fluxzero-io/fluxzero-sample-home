package io.fluxzero.home.scenes.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.With;

/** Descriptive information about a scene, independent of its identity and current state. */
@With
public record SceneDetails(@NotBlank @Size(max = 120) String name) {}
