package io.fluxzero.home.homeassistant.api;

import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.homeassistant.api.model.HomeAssistantDevice;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.persisting.eventsourcing.InterceptApply;
import jakarta.annotation.Nullable;

/** Current delivery failure, tied to the still-selected connection. */
public record RecordHomeAssistantDeliveryProblem(DeviceId deviceId, HomeAssistantId connectionId, String problem) {
    @InterceptApply Object ifStillLinked(@Nullable HomeAssistantDevice binding) {
        return binding != null && binding.connectionId().equals(connectionId) ? this : null;
    }
    @Apply HomeAssistantDevice apply(HomeAssistantDevice binding) { return binding.withProblem(problem); }
}
