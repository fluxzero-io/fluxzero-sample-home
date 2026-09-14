package io.fluxzero.home.automation;

import io.fluxzero.home.command.ActivateScene;
import io.fluxzero.home.model.Automation;
import io.fluxzero.home.model.AutomationId;
import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.HomeChange;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.common.Message;
import io.fluxzero.sdk.common.exception.FunctionalException;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.persisting.eventsourcing.InterceptApply;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.List;

/** React to a household change and commit the scene together with its execution history. */
public record ReactToHome(AutomationId automationId, HomeChange change) {
    @InterceptApply Object prepare(@Nullable Automation automation, Graph<Home> home, Message message) {
        if (automation == null || !automation.enabled() || change.at().isBefore(automation.createdAt())
                || !shouldActivate(automation, message.getTimestamp())) return null;
        try {
            Fluxzero.assertLegal(new ActivateScene(automation.sceneId()));
            return List.of(new ActivateScene(automation.sceneId()), this);
        } catch (FunctionalException failure) {
            return new PauseFailedAutomation(automationId, failure.getMessage());
        }
    }
    @Apply Automation apply(Automation automation, Message message) {
        return automation.withExecutionCount(automation.executionCount() + 1).withLastExecutedAt(message.getTimestamp());
    }
    private boolean shouldActivate(Automation automation, Instant at) {
        return automation.trigger().matches(change) && (automation.lastExecutedAt() == null
                || !at.isBefore(automation.lastExecutedAt().plus(automation.cooldown())));
    }
}
