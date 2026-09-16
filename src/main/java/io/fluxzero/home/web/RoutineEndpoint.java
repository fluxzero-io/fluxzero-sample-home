package io.fluxzero.home.web;

import io.fluxzero.home.access.BrowserRequests;
import io.fluxzero.home.access.HomeAccess;
import io.fluxzero.home.access.HomePermission;
import io.fluxzero.home.access.HomeUser;
import io.fluxzero.home.command.PauseRoutine;
import io.fluxzero.home.command.PlanRoutine;
import io.fluxzero.home.command.RemoveRoutine;
import io.fluxzero.home.command.ResumeRoutine;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.home.model.Routine;
import io.fluxzero.home.model.RoutineDetails;
import io.fluxzero.home.model.RoutineId;
import io.fluxzero.home.model.RoutineTiming;
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
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

@Component
@Path("homes/{homeId}/routines/{routineId}")
public class RoutineEndpoint {
    @HandlePut @ApiDoc(operationId = "planRoutine")
    void plan(@PathParam HomeId homeId, @PathParam RoutineId routineId, Definition body, HomeUser user, WebRequest request) {
        BrowserRequests.requireSameOrigin(request);
        HomeAccess.require(homeId, user, HomePermission.MANAGE);
        Fluxzero.sendCommandAndWait(new PlanRoutine(routineId, homeId, body.details(), body.sceneId(), body.timing()));
    }
    @HandlePost("/pause") @ApiDoc(operationId = "pauseRoutine")
    void pause(@PathParam HomeId homeId, @PathParam RoutineId routineId, HomeUser user, WebRequest request) {
        requireRoutine(homeId, routineId, user, request);
        Fluxzero.sendCommandAndWait(new PauseRoutine(routineId));
    }
    @HandlePost("/resume") @ApiDoc(operationId = "resumeRoutine")
    void resume(@PathParam HomeId homeId, @PathParam RoutineId routineId, HomeUser user, WebRequest request) {
        requireRoutine(homeId, routineId, user, request);
        Fluxzero.sendCommandAndWait(new ResumeRoutine(routineId));
    }
    @HandleDelete @ApiDoc(operationId = "removeRoutine")
    void remove(@PathParam HomeId homeId, @PathParam RoutineId routineId, HomeUser user, WebRequest request) {
        requireRoutine(homeId, routineId, user, request);
        Fluxzero.sendCommandAndWait(new RemoveRoutine(routineId));
    }
    private static void requireRoutine(HomeId homeId, RoutineId routineId, HomeUser user, WebRequest request) {
        BrowserRequests.requireSameOrigin(request);
        if (HomeAccess.require(homeId, user, HomePermission.MANAGE).find(routineId, Routine.class).isEmpty()) {
            throw new UnauthorizedException("Choose a routine in this home.");
        }
    }
    public record Definition(@ApiDoc(required = true) @NotNull @Valid RoutineDetails details,
                              @ApiDoc(required = true) @NotNull SceneId sceneId,
                              @ApiDoc(required = true) @NotNull @Valid RoutineTiming timing) {}
}
