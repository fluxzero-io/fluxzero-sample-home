package io.fluxzero.home.command;

import io.fluxzero.home.model.Automation;
import io.fluxzero.home.model.AutomationId;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Allow future changes to trigger the automation again. */
public record ResumeAutomation(AutomationId automationId) {
    @Apply Automation apply(Automation automation) {
        return automation.enabled() ? automation : automation.withEnabled(true).withProblem(null);
    }
}
