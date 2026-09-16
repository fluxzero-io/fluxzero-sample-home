package io.fluxzero.home.access;

import io.fluxzero.home.access.api.AccountId;
import io.fluxzero.home.access.api.model.Account;
import io.fluxzero.home.access.api.model.HomePermission;
import io.fluxzero.home.access.api.model.HomeUser;
import io.fluxzero.home.household.api.HomeId;
import io.fluxzero.home.household.api.model.Home;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.tracking.handling.authentication.UnauthorizedException;

/** Household permissions are checked server-side against current application state. */
public final class HomeAccess {
    private HomeAccess() {}

    public static HomePermission permission(HomeId homeId, HomeUser user) {
        Account account = user == null ? null : Fluxzero.loadCurrentGraph(new AccountId(user.id())).get();
        HomePermission permission = account == null ? null : account.homes().get(homeId);
        if (permission == null) throw new UnauthorizedException("This home is not available to your account.");
        return permission;
    }

    public static Graph<Home> require(HomeId homeId, HomeUser user, HomePermission minimum) {
        if (permission(homeId, user).ordinal() < minimum.ordinal()) {
            throw new UnauthorizedException("Your access does not allow this action.");
        }
        Graph<Home> home = Fluxzero.loadCurrentGraph(homeId);
        if (home.isEmpty()) throw new UnauthorizedException("This home is no longer available.");
        return home;
    }
}
