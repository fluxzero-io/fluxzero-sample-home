package io.fluxzero.home.household.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.With;

/** Descriptive information about a zone, independent of its identity and current state. */
@With
public record ZoneDetails(@NotBlank @Size(max = 120) String name) {}
