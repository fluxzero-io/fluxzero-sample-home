package io.fluxzero.home.access;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountDetails(@NotBlank @Size(max = 120) String name) {}
