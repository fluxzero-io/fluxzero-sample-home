package io.fluxzero.home.automation;

import io.fluxzero.home.model.Automation;
import io.fluxzero.home.model.AutomationId;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Keep a failed automatic reaction visible, with no partial device changes. */
public record PauseFailedAutomation(AutomationId automationId, String problem) {
    @Apply Automation apply(Automation automation) {
        return automation.withEnabled(false).withProblem(problem);
    }
}
