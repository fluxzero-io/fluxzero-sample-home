package io.fluxzero.home.command;

import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.home.model.HomeMode;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import jakarta.validation.constraints.NotNull;

/** Express whether the household is home, away, asleep or on holiday. */
public record ChangeHomeMode(HomeId homeId, @NotNull(message = "Choose how the home is being used.") HomeMode mode) {
    @Apply Home apply(Home home) { return home.withMode(mode); }
}
