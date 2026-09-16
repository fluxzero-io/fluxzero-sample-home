package io.fluxzero.home.household.api.model;

import io.fluxzero.home.household.api.HomeId;
import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import java.time.ZoneId;
import lombok.With;

/** A household’s home, its local time zone and current way of living. */
@Model
@With
public record Home(@EntityId HomeId id, HomeDetails details, ZoneId timeZone, HomeMode mode) implements Place {}
