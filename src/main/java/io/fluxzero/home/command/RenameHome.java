package io.fluxzero.home.command;

import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Change what the household calls its home. */
public record RenameHome(HomeId homeId, @NotBlank @Size(max = 120) String name) {
    @Apply Home apply(Home home) { return home.withDetails(home.details().withName(name)); }
}
