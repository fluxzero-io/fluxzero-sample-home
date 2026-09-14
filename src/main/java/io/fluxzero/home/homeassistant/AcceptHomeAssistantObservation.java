package io.fluxzero.home.homeassistant;

import io.fluxzero.home.command.ReportDeviceStatus;
import io.fluxzero.home.model.DeviceId;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.persisting.eventsourcing.InterceptApply;
import io.fluxzero.sdk.tracking.TrackSelf;
import io.fluxzero.sdk.tracking.handling.HandleCommand;
import jakarta.annotation.Nullable;
import java.util.Set;

/** An observation can outlive an HTTP request's selected route; accept it only while that route still belongs here. */
@TrackSelf
record AcceptHomeAssistantObservation(DeviceId deviceId, HomeAssistantId connectionId, Set<String> entityIds,
                                      ReportDeviceStatus report) {
    @HandleCommand void handle() { Fluxzero.assertAndApply(this); }

    @InterceptApply
    Object ifStillLinked(@Nullable HomeAssistantDevice binding) {
        return binding != null && binding.connectionId().equals(connectionId) && binding.entityIds().equals(entityIds)
                ? report : null;
    }
}
