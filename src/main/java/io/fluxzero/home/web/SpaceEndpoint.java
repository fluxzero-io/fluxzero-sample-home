package io.fluxzero.home.web;

import io.fluxzero.home.access.BrowserRequests;
import io.fluxzero.home.access.HomeAccess;
import io.fluxzero.home.access.HomePermission;
import io.fluxzero.home.access.HomeUser;
import io.fluxzero.home.command.AddSpace;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.home.model.Space;
import io.fluxzero.home.model.SpaceDetails;
import io.fluxzero.home.model.SpaceId;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.tracking.handling.authentication.UnauthorizedException;
import io.fluxzero.sdk.web.ApiDoc;
import io.fluxzero.sdk.web.ApiDocResponse;
import io.fluxzero.sdk.web.HandlePost;
import io.fluxzero.sdk.web.Path;
import io.fluxzero.sdk.web.PathParam;
import io.fluxzero.sdk.web.WebRequest;
import io.fluxzero.sdk.web.WebResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

@Component
@Path("homes/{homeId}/spaces")
public class SpaceEndpoint {
    @HandlePost @ApiDoc(operationId = "addSpace")
    @ApiDocResponse(status = 201, type = CreatedSpace.class)
    WebResponse add(@PathParam HomeId homeId, Definition body, HomeUser user, WebRequest request) {
        BrowserRequests.requireSameOrigin(request);
        var home = HomeAccess.require(homeId, user, HomePermission.MANAGE);
        if (body.enclosingSpaceId() != null && home.find(body.enclosingSpaceId(), Space.class).isEmpty()) {
            throw new UnauthorizedException("Choose a location in this home.");
        }
        var spaceId = new SpaceId(Fluxzero.generateId());
        Fluxzero.sendCommandAndWait(new AddSpace(spaceId,
                body.enclosingSpaceId() == null ? homeId : body.enclosingSpaceId(), body.details()));
        return WebResponse.builder().status(201).payload(new CreatedSpace(spaceId)).build();
    }

    public record Definition(@ApiDoc(required = true) @NotNull @Valid SpaceDetails details,
                             SpaceId enclosingSpaceId) {}
    public record CreatedSpace(SpaceId spaceId) {}
}
