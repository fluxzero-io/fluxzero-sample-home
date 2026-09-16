package io.fluxzero.home.devices.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.With;

/** Descriptive information about a device, independent of its identity and current state. */
@With
public record DeviceDetails(@NotBlank @Size(max = 120) String name) {}
