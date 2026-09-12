package io.fluxzero.home.model;

import io.fluxzero.sdk.modeling.EntityId;
import io.fluxzero.sdk.modeling.Model;
import lombok.With;

import static io.fluxzero.sdk.modeling.ModelPersistence.DOCUMENT;
import static io.fluxzero.sdk.modeling.ModelPersistence.EVENT_SOURCED;

/** A household’s home, its local time zone and current way of living. */
@Model(persistence = {EVENT_SOURCED, DOCUMENT})
@With
public record Home(@EntityId HomeId homeId, String name, java.time.ZoneId timeZone, HomeMode mode) {}
