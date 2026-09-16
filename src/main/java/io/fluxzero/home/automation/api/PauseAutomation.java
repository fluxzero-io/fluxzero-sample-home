package io.fluxzero.home.automation.api;

import io.fluxzero.home.automation.api.model.Automation;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Pause reactions without forgetting their history. */
public record PauseAutomation(AutomationId automationId) {
    @Apply Automation apply(Automation automation) { return automation.withEnabled(false); }
}
