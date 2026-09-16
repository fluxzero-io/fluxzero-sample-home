package io.fluxzero.home.homeassistant.api;

import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.devices.api.ReportDeviceStatus;
import io.fluxzero.home.homeassistant.api.model.HomeAssistantDevice;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.persisting.eventsourcing.InterceptApply;
import io.fluxzero.sdk.tracking.TrackSelf;
import io.fluxzero.sdk.tracking.handling.HandleCommand;
import jakarta.annotation.Nullable;
import java.util.Set;

/** An observation can outlive an HTTP request's selected route; accept it only while that route still belongs here. */
@TrackSelf
public record AcceptHomeAssistantObservation(DeviceId deviceId, HomeAssistantId connectionId, Set<String> entityIds,
                                      ReportDeviceStatus report) {
    @HandleCommand void handle() { Fluxzero.assertAndApply(this); }

    @InterceptApply
    Object ifStillLinked(@Nullable HomeAssistantDevice binding) {
        return binding != null && binding.connectionId().equals(connectionId) && binding.entityIds().equals(entityIds)
                ? report : null;
    }
}
