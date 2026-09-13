package io.fluxzero.home.model;

import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import lombok.With;

/** A household’s home, its local time zone and current way of living. */
@Model
@With
public record Home(@EntityId HomeId homeId, HomeDetails details, java.time.ZoneId timeZone, HomeMode mode) {}
