package io.fluxzero.home.automation.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.With;

/** Descriptive information about a routine, independent of its identity and current state. */
@With
public record RoutineDetails(@NotBlank @Size(max = 120) String name) {}
