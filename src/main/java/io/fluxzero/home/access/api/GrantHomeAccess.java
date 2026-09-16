package io.fluxzero.home.access.api;

import io.fluxzero.home.access.api.model.Account;
import io.fluxzero.home.access.api.model.AccountDetails;
import io.fluxzero.home.access.api.model.HomePermission;
import io.fluxzero.home.household.api.HomeId;
import io.fluxzero.home.household.api.model.Home;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.tracking.handling.authentication.RequiresAnyRole;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashMap;
import java.util.Map;

/** Operator provisioning; never exposed as an anonymous HTTP or login operation. */
@RequiresAnyRole("SYSTEM")
public record GrantHomeAccess(@NotNull AccountId accountId, @NotNull @Valid AccountDetails details,
                              @NotNull HomeId homeId, @NotNull HomePermission permission) {
    @AssertTrue(message = "Choose an ordinary identity-provider subject.")
    boolean hasNonReservedSubject() { return !accountId.getId().startsWith("$"); }
    @Apply Account apply(@Nullable Account account, Home home) {
        var homes = new LinkedHashMap<HomeId, HomePermission>();
        if (account != null) homes.putAll(account.homes());
        homes.put(homeId, permission);
        return new Account(accountId, details, Map.copyOf(homes));
    }
}
