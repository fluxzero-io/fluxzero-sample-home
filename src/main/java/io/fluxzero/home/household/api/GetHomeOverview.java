package io.fluxzero.home.household.api;

import io.fluxzero.home.access.HomeAccess;
import io.fluxzero.home.access.api.model.HomePermission;
import io.fluxzero.home.access.api.model.HomeUser;
import io.fluxzero.home.household.api.model.HomeOverview;
import io.fluxzero.sdk.tracking.handling.HandleQuery;
import io.fluxzero.sdk.tracking.handling.Request;
import io.fluxzero.sdk.tracking.handling.authentication.RequiresUser;

@RequiresUser
public record GetHomeOverview(HomeId homeId) implements Request<HomeOverview> {
    @HandleQuery HomeOverview handle(HomeUser user) {
        return HomeOverview.from(HomeAccess.require(homeId, user, HomePermission.VIEW), HomeAccess.permission(homeId, user));
    }
}
