package io.fluxzero.home.model;

import io.fluxzero.common.serialization.Revision;
import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import lombok.With;

/** A household’s home, its local time zone and current way of living. */
@Model
@Revision(1)
@With
public record Home(@EntityId HomeId homeId, HomeDetails details, java.time.ZoneId timeZone, HomeMode mode) {}
