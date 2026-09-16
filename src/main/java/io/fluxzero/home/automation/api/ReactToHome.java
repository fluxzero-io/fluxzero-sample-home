package io.fluxzero.home.automation.api;

import io.fluxzero.home.automation.api.model.Automation;
import io.fluxzero.home.automation.api.model.HomeChange;
import io.fluxzero.home.scenes.api.ActivateScene;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.common.Message;
import io.fluxzero.sdk.common.exception.FunctionalException;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.persisting.eventsourcing.InterceptApply;
import io.fluxzero.sdk.tracking.handling.HandleCommand;
import jakarta.annotation.Nullable;
import java.util.List;

import static io.fluxzero.sdk.modeling.EventPublication.ALWAYS;

/** React to a household change and commit the scene together with its quiet interval. */
public record ReactToHome(AutomationId automationId, HomeChange change) {
    @HandleCommand
    void execute() {
        try {
            Fluxzero.assertAndApply(this);
        } catch (FunctionalException failure) {
            Fluxzero.assertAndApply(new PauseFailedAutomation(automationId, change, failure.getMessage()));
        }
    }

    @InterceptApply Object prepare(@Nullable Automation automation, Message message) {
        if (automation == null || !automation.enabled() || change.at().isBefore(automation.effectiveFrom())
                || !automation.trigger().matches(change)
                || automation.cooldownEndsAt() != null && message.getTimestamp().isBefore(automation.cooldownEndsAt())) {
            return null;
        }
        return List.of(new ActivateScene(automation.sceneId()), this);
    }

    @Apply(eventPublication = ALWAYS)
    Automation apply(Automation automation, Message message) {
        return automation.withCooldownEndsAt(message.getTimestamp().plus(automation.cooldown()));
    }
}
