package io.fluxzero.home.command;

import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.home.model.HomeMode;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

import static io.fluxzero.home.model.Rules.require;

/** Express whether the household is home, away, asleep or on holiday. */
public record ChangeHomeMode(HomeId homeId, HomeMode mode) {
    @AssertLegal void validate() { require(mode != null, "Choose how the home is being used."); }
    @Apply Home apply(Home home) { return home.withMode(mode); }
}
