package io.fluxzero.home.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.With;

/** Descriptive information about a space, independent of its identity and current state. */
@With
public record SpaceDetails(@NotBlank @Size(max = 120) String name, @NotNull SpaceKind kind) {}
