package io.fluxzero.home.homeassistant;

import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.persisting.eventsourcing.InterceptApply;
import jakarta.annotation.Nullable;

/** Current refresh failure; a successful refresh clears it. Contains only sanitized messages. */
record RecordHomeAssistantProblem(HomeAssistantId connectionId, String problem) {
    @InterceptApply Object ifStillConnected(@Nullable HomeAssistantConnection connection) { return connection == null ? null : this; }
    @Apply HomeAssistantConnection apply(HomeAssistantConnection connection) { return connection.withProblem(problem); }
}
