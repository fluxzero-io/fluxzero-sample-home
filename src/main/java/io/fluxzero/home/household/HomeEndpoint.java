package io.fluxzero.home.household;

import io.fluxzero.home.access.BrowserRequests;
import io.fluxzero.home.access.HomeAccess;
import io.fluxzero.home.access.api.model.HomePermission;
import io.fluxzero.home.access.api.model.HomeUser;
import io.fluxzero.home.household.api.ChangeHomeMode;
import io.fluxzero.home.household.api.GetHomeOverview;
import io.fluxzero.home.household.api.HomeId;
import io.fluxzero.home.household.api.model.HomeMode;
import io.fluxzero.home.household.api.model.HomeOverview;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.tracking.handling.authentication.RequiresUser;
import io.fluxzero.sdk.web.ApiDoc;
import io.fluxzero.sdk.web.ApiDocResponse;
import io.fluxzero.sdk.web.HandleGet;
import io.fluxzero.sdk.web.HandlePut;
import io.fluxzero.sdk.web.Path;
import io.fluxzero.sdk.web.PathParam;
import io.fluxzero.sdk.web.WebRequest;
import io.fluxzero.sdk.web.WebResponse;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

@Component
@RequiresUser
@ApiDoc
@Path("/api/homes/{homeId}")
public class HomeEndpoint {
    @HandleGet @ApiDoc(operationId = "getHome")
    @ApiDocResponse(status = 200, type = HomeOverview.class)
    WebResponse get(@PathParam HomeId homeId) {
        return WebResponse.builder().payload(Fluxzero.queryAndWait(new GetHomeOverview(homeId)))
                .header("Cache-Control", "no-store").build();
    }

    @HandlePut("/mode") @ApiDoc(operationId = "changeHomeMode")
    void mode(@PathParam HomeId homeId, ModeRequest body, HomeUser user, WebRequest request) {
        BrowserRequests.requireSameOrigin(request);
        HomeAccess.require(homeId, user, HomePermission.CONTROL);
        Fluxzero.sendCommandAndWait(new ChangeHomeMode(homeId, body.mode()));
    }
    public record ModeRequest(@ApiDoc(required = true) @NotNull HomeMode mode) {}
}
