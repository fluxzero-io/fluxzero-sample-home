package io.fluxzero.home.automation.api;

import io.fluxzero.home.automation.api.model.Automation;
import io.fluxzero.home.automation.api.model.HomeChange;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.persisting.eventsourcing.InterceptApply;
import jakarta.annotation.Nullable;

/** Keep a failed automatic reaction visible, with no partial device changes. */
public record PauseFailedAutomation(AutomationId automationId, HomeChange change, String problem) {
    @InterceptApply Object ignoreObsolete(@Nullable Automation automation) {
        return automation == null || !automation.enabled() || change.at().isBefore(automation.effectiveFrom())
                || !automation.trigger().matches(change) ? null : this;
    }

    @Apply Automation apply(Automation automation) {
        return automation.withEnabled(false).withProblem(problem);
    }
}
