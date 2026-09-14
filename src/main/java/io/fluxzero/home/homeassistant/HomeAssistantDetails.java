package io.fluxzero.home.homeassistant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** A friendly name and a reference to operator-managed connection configuration. Never contains a token. */
public record HomeAssistantDetails(@NotBlank @Size(max = 120) String name,
                                   @NotBlank @Pattern(regexp = "[a-z][a-z0-9-]*") String configuration) {}
