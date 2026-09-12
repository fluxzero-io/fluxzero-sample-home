package io.fluxzero.home.command;

import io.fluxzero.home.model.Automation;
import io.fluxzero.home.model.AutomationId;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Remove an automation from the home. */
public record RemoveAutomation(AutomationId automationId) {
    @Apply Automation apply(Automation automation) { return null; }
}
