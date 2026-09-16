package io.fluxzero.home.web;

import io.fluxzero.home.access.BrowserRequests;
import io.fluxzero.home.access.HomeAccess;
import io.fluxzero.home.access.HomePermission;
import io.fluxzero.home.access.HomeUser;
import io.fluxzero.home.command.ChangeHomeMode;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.home.model.HomeMode;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.web.ApiDoc;
import io.fluxzero.sdk.web.ApiDocResponse;
import io.fluxzero.sdk.web.WebResponse;
import io.fluxzero.sdk.web.HandleGet;
import io.fluxzero.sdk.web.HandlePut;
import io.fluxzero.sdk.web.Path;
import io.fluxzero.sdk.web.PathParam;
import io.fluxzero.sdk.web.WebRequest;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

@Component
@Path("homes/{homeId}")
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
