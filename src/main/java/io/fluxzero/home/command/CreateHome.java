package io.fluxzero.home.command;

import io.fluxzero.common.serialization.Revision;
import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.HomeDetails;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.home.model.HomeMode;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import static io.fluxzero.home.model.Rules.require;

/** Start a household’s home in its own local time zone. */
@Revision(1)
public record CreateHome(HomeId homeId, @NotNull @Valid HomeDetails details, java.time.ZoneId timeZone) {
    @AssertLegal void validate() { require(homeId != null && timeZone != null, "A home needs an identity and time zone."); }
    @Apply Home apply() { return new Home(homeId, details, timeZone, HomeMode.HOME); }
}
