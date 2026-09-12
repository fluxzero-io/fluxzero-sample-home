package io.fluxzero.home.command;

import io.fluxzero.home.model.Automation;
import io.fluxzero.home.model.AutomationId;
import io.fluxzero.home.model.Home;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;

/** Allow future changes to trigger the automation again. */
public record ResumeAutomation(AutomationId automationId) {
    @Apply Automation apply(Automation automation, Graph<Home> home) {
        return automation.enabled() ? automation : automation.withEnabled(true).withProblem(null)
                .withLastProcessedRevision(automation.trigger().sourceRevision(home));
    }
}
