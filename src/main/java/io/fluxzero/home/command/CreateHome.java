package io.fluxzero.home.command;

import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.home.model.HomeMode;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import jakarta.annotation.Nullable;

import static io.fluxzero.home.model.Rules.named;
import static io.fluxzero.home.model.Rules.require;

/** Start a household’s home in its own local time zone. */
public record CreateHome(HomeId homeId, String name, java.time.ZoneId timeZone) {
    @AssertLegal void validate() { named(name); require(homeId != null && timeZone != null, "A home needs an identity and time zone."); }
    @AssertLegal void requireNew(@Nullable Home existing) {
        require(existing == null, "This home already exists.");
    }
    @Apply Home apply() { return new Home(homeId, name, timeZone, HomeMode.HOME); }
}
