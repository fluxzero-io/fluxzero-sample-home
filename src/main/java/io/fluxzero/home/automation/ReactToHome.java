package io.fluxzero.home.automation;

import io.fluxzero.home.model.Automation;
import io.fluxzero.home.model.AutomationId;
import io.fluxzero.home.model.DeviceStatus;
import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.HomeRuleViolation;
import io.fluxzero.home.model.Scene;
import io.fluxzero.home.model.ScenePlan;
import io.fluxzero.sdk.common.Message;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.persisting.eventsourcing.InterceptApply;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.List;

/** Process one source revision and commit an optional scene in the same transaction. */
public record ReactToHome(AutomationId automationId, HomeSignal signal) {
    @InterceptApply Object prepare(@Nullable Automation automation, Graph<Home> home, Message message) {
        if (automation == null || !automation.enabled() || signal.at().isBefore(automation.createdAt())
                || signal.revision() <= automation.lastProcessedRevision()) return null;
        var source = signal.statusId() == null ? home : home.find(signal.statusId(), DeviceStatus.class).orElse(null);
        if (source == null || source.revisionStateIndex() != signal.revision()) return null;
        if (!shouldActivate(automation, message.getTimestamp())) return this;
        try {
            var scene = ScenePlan.find(home, automation.sceneId(), Scene.class).get();
            ScenePlan.devices(scene.actions(), home);
            return List.of(new io.fluxzero.home.command.ActivateScene(automation.sceneId()), this);
        } catch (HomeRuleViolation failure) {
            return new PauseFailedAutomation(automationId, signal.revision(), failure.getMessage());
        }
    }
    @Apply Automation apply(Automation automation, Message message) {
        var next = automation.withLastProcessedRevision(signal.revision());
        return shouldActivate(automation, message.getTimestamp())
                ? next.withExecutionCount(next.executionCount() + 1).withLastExecutedAt(message.getTimestamp()) : next;
    }
    private boolean shouldActivate(Automation automation, Instant at) {
        return signal.matches(automation.trigger()) && (automation.lastExecutedAt() == null
                || !at.isBefore(automation.lastExecutedAt().plus(automation.cooldown())));
    }
}
