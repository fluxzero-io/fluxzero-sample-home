package io.fluxzero.home.automation.api;

import io.fluxzero.home.automation.api.model.Automation;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Remove an automation from the home. */
public record RemoveAutomation(AutomationId automationId) {
    @Apply Automation apply(Automation automation) { return null; }
}
