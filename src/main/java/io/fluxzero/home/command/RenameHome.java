package io.fluxzero.home.command;

import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

import static io.fluxzero.home.model.Rules.named;

/** Change what the household calls its home. */
public record RenameHome(HomeId homeId, String name) {
    @AssertLegal void validate() { named(name); }
    @Apply Home apply(Home home) { return home.withDetails(home.details().withName(name)); }
}
