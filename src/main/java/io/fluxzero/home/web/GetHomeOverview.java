package io.fluxzero.home.web;

import io.fluxzero.home.access.HomeAccess;
import io.fluxzero.home.access.HomePermission;
import io.fluxzero.home.access.HomeUser;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.sdk.tracking.handling.HandleQuery;
import io.fluxzero.sdk.tracking.handling.Request;

public record GetHomeOverview(HomeId homeId) implements Request<HomeOverview> {
    @HandleQuery HomeOverview handle(HomeUser user) {
        return HomeOverview.from(HomeAccess.require(homeId, user, HomePermission.VIEW), HomeAccess.permission(homeId, user));
    }
}
