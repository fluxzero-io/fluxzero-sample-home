package io.fluxzero.home.homeassistant;

import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;

/** Connect an operator-configured installation to this home; discovering entities does not import devices. */
public record ConnectHomeAssistant(HomeAssistantId connectionId, HomeId homeId,
                                   @NotNull @Valid HomeAssistantDetails details,
                                   @NotNull Duration refreshInterval) {
    @AssertTrue(message = "Refresh between every five seconds and every hour.")
    boolean hasReasonableRefreshInterval() {
        return refreshInterval.compareTo(Duration.ofSeconds(5)) >= 0
                && refreshInterval.compareTo(Duration.ofHours(1)) <= 0;
    }

    @Apply
    HomeAssistantConnection apply(Home home) {
        return new HomeAssistantConnection(connectionId, homeId, details, refreshInterval, null);
    }
}
