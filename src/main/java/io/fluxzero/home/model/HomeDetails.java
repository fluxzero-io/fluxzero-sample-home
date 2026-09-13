package io.fluxzero.home.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.With;

/** Descriptive information about a home, independent of its identity and current state. */
@With
public record HomeDetails(@NotBlank @Size(max = 120) String name) {}
