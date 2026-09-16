package io.fluxzero.home.web;

import io.fluxzero.home.access.BrowserRequests;
import io.fluxzero.home.access.HomeAccess;
import io.fluxzero.home.access.HomePermission;
import io.fluxzero.home.access.HomeUser;
import io.fluxzero.home.command.ActivateScene;
import io.fluxzero.home.command.DefineScene;
import io.fluxzero.home.command.RemoveScene;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.home.model.Scene;
import io.fluxzero.home.model.SceneAction;
import io.fluxzero.home.model.SceneDetails;
import io.fluxzero.home.model.SceneId;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.tracking.handling.authentication.UnauthorizedException;
import io.fluxzero.sdk.web.ApiDoc;
import io.fluxzero.sdk.web.HandleDelete;
import io.fluxzero.sdk.web.HandlePost;
import io.fluxzero.sdk.web.HandlePut;
import io.fluxzero.sdk.web.Path;
import io.fluxzero.sdk.web.PathParam;
import io.fluxzero.sdk.web.WebRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Path("homes/{homeId}/scenes/{sceneId}")
public class SceneEndpoint {
    @HandlePost("/activate") @ApiDoc(operationId = "activateScene")
    void activate(@PathParam HomeId homeId, @PathParam SceneId sceneId, HomeUser user, WebRequest request) {
        requireScene(homeId, sceneId, user, request, HomePermission.CONTROL);
        Fluxzero.sendCommandAndWait(new ActivateScene(sceneId));
    }
    @HandlePut @ApiDoc(operationId = "defineScene")
    void define(@PathParam HomeId homeId, @PathParam SceneId sceneId, Definition body, HomeUser user, WebRequest request) {
        BrowserRequests.requireSameOrigin(request);
        HomeAccess.require(homeId, user, HomePermission.MANAGE);
        Fluxzero.sendCommandAndWait(new DefineScene(sceneId, homeId, body.details(), body.actions()));
    }
    @HandleDelete @ApiDoc(operationId = "removeScene")
    void remove(@PathParam HomeId homeId, @PathParam SceneId sceneId, HomeUser user, WebRequest request) {
        requireScene(homeId, sceneId, user, request, HomePermission.MANAGE);
        Fluxzero.sendCommandAndWait(new RemoveScene(sceneId));
    }
    private static void requireScene(HomeId homeId, SceneId sceneId, HomeUser user, WebRequest request, HomePermission permission) {
        BrowserRequests.requireSameOrigin(request);
        if (HomeAccess.require(homeId, user, permission).find(sceneId, Scene.class).isEmpty()) {
            throw new UnauthorizedException("Choose a scene in this home.");
        }
    }
    public record Definition(@ApiDoc(required = true) @NotNull @Valid SceneDetails details,
                              @ApiDoc(required = true) @NotEmpty @Valid List<@NotNull SceneAction> actions) {}
}
