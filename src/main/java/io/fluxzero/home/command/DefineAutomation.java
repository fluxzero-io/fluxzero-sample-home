package io.fluxzero.home.command;

import io.fluxzero.home.model.Automation;
import io.fluxzero.home.model.AutomationDetails;
import io.fluxzero.home.model.AutomationId;
import io.fluxzero.home.model.AutomationTrigger;
import io.fluxzero.home.model.Device;
import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.home.model.HomeRuleViolation;
import io.fluxzero.home.model.Scene;
import io.fluxzero.home.model.SceneId;
import io.fluxzero.sdk.common.Message;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;

import static io.fluxzero.home.model.Rules.require;

/** Describe when a scene should react, with an optional quiet interval between activations. */
public record DefineAutomation(AutomationId automationId, HomeId homeId, @NotNull @Valid AutomationDetails details,
                               @NotNull SceneId sceneId,
                               @NotNull @Valid AutomationTrigger trigger, @NotNull Duration cooldown) {
    @AssertTrue(message = "Choose a non-negative cooldown.")
    boolean hasNonNegativeCooldown() {
        return cooldown == null || !cooldown.isNegative();
    }

    @AssertLegal void validate(Graph<Home> home, @Nullable Automation automation) {
        require(automation == null || automation.homeId().equals(homeId), "An automation cannot move between homes.");
        require(home.find(sceneId, Scene.class).isPresent(),
                "Choose an existing scene from this home.");
        if (trigger instanceof AutomationTrigger.MeasurementCrosses crossing) {
            var device = home.find(crossing.deviceId(), Device.class)
                    .orElseThrow(() -> new HomeRuleViolation("Choose an existing device from this home.")).get();
            require(device.measurements().contains(crossing.measurement()), "Choose a measurement supplied by this device.");
        }
    }
    @Apply Automation apply(@Nullable Automation automation, Message message) {
        return new Automation(automationId, homeId, details, sceneId, trigger, cooldown, true, message.getTimestamp(),
                automation == null ? 0 : automation.executionCount(), automation == null ? null : automation.lastExecutedAt(), null);
    }
}
