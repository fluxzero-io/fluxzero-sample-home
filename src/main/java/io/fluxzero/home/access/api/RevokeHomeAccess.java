package io.fluxzero.home.access.api;

import io.fluxzero.home.access.api.model.Account;
import io.fluxzero.home.household.api.HomeId;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.tracking.handling.authentication.RequiresAnyRole;
import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashMap;
import java.util.Map;

/** Operator-managed membership removal; existing sessions lose access on their next request or update. */
@RequiresAnyRole("SYSTEM")
public record RevokeHomeAccess(@NotNull AccountId accountId, @NotNull HomeId homeId) {
    @Apply Account apply(Account account) {
        var homes = new LinkedHashMap<>(account.homes());
        homes.remove(homeId);
        return new Account(accountId, account.details(), Map.copyOf(homes));
    }
}
