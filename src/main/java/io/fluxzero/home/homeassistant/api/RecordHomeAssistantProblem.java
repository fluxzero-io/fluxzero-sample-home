package io.fluxzero.home.homeassistant.api;

import io.fluxzero.home.homeassistant.api.model.HomeAssistantConnection;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.persisting.eventsourcing.InterceptApply;
import jakarta.annotation.Nullable;

/** Current refresh failure; a successful refresh clears it. Contains only sanitized messages. */
public record RecordHomeAssistantProblem(HomeAssistantId connectionId, String problem) {
    @InterceptApply Object ifStillConnected(@Nullable HomeAssistantConnection connection) { return connection == null ? null : this; }
    @Apply HomeAssistantConnection apply(HomeAssistantConnection connection) { return connection.withProblem(problem); }
}
