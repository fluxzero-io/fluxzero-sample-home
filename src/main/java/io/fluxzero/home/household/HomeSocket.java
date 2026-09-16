package io.fluxzero.home.household;

import io.fluxzero.common.api.Metadata;
import io.fluxzero.home.access.BrowserRequests;
import io.fluxzero.home.access.BrowserSessions;
import io.fluxzero.home.access.HomeAccess;
import io.fluxzero.home.access.api.model.HomePermission;
import io.fluxzero.home.access.api.model.HomeUser;
import io.fluxzero.home.household.api.HomeId;
import io.fluxzero.home.household.api.model.Home;
import io.fluxzero.home.household.api.model.HomeOverview;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.tracking.handling.HandleNotification;
import io.fluxzero.sdk.tracking.handling.authentication.RequiresUser;
import io.fluxzero.sdk.tracking.handling.authentication.UnauthorizedException;
import io.fluxzero.sdk.web.ApiDoc;
import io.fluxzero.sdk.web.HandleSocketMessage;
import io.fluxzero.sdk.web.HandleSocketOpen;
import io.fluxzero.sdk.web.Path;
import io.fluxzero.sdk.web.PathParam;
import io.fluxzero.sdk.web.SocketEndpoint;
import io.fluxzero.sdk.web.SocketSession;
import io.fluxzero.sdk.web.WebRequest;

/** One SDK-owned instance per viewer, with access and session validity rechecked before each snapshot. */
@SocketEndpoint
@RequiresUser
@ApiDoc
@Path("/api/homes/{homeId}/live")
public record HomeSocket(SocketSession session, HomeId homeId, HomeUser user, Metadata credentials) {
    @HandleSocketOpen
    static HomeSocket open(SocketSession session, @PathParam HomeId homeId, HomeUser user, WebRequest request) {
        BrowserRequests.requireTrustedOrigin(request);
        HomeAccess.require(homeId, user, HomePermission.VIEW);
        HomeSocket socket = new HomeSocket(session, homeId, user, request.getMetadata());
        socket.refresh();
        return socket;
    }

    @HandleNotification
    void changed(Graph<Home> home) {
        if (homeId.getId().equals(home.functionalId())) refresh();
    }

    @HandleSocketMessage
    void refresh() {
        if (BrowserSessions.find(credentials).isEmpty()) { session.close(1008); return; }
        try {
            session.sendMessage(HomeOverview.from(HomeAccess.require(homeId, user, HomePermission.VIEW),
                    HomeAccess.permission(homeId, user)));
        } catch (UnauthorizedException e) {
            session.close(1008);
        }
    }
}
