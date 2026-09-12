package io.fluxzero.home.command;

import io.fluxzero.home.model.Automation;
import io.fluxzero.home.model.AutomationId;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Pause reactions without forgetting their history. */
public record PauseAutomation(AutomationId automationId) {
    @Apply Automation apply(Automation automation) { return automation.withEnabled(false); }
}
