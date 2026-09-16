package io.fluxzero.home.household.api;

import io.fluxzero.home.household.api.model.Home;
import io.fluxzero.home.household.api.model.HomeDetails;
import io.fluxzero.home.household.api.model.HomeMode;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.ZoneId;

/** Start a household’s home in its own local time zone. */
public record CreateHome(@NotNull HomeId homeId, @NotNull @Valid HomeDetails details, @NotNull ZoneId timeZone) {
    @Apply Home apply() { return new Home(homeId, details, timeZone, HomeMode.HOME); }
}
