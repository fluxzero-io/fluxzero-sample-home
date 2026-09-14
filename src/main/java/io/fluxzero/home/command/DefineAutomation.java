package io.fluxzero.home.command;

import io.fluxzero.home.model.Automation;
import io.fluxzero.home.model.AutomationDetails;
import io.fluxzero.home.model.AutomationId;
import io.fluxzero.home.model.AutomationTrigger;
import io.fluxzero.home.model.Device;
import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.home.model.Scene;
import io.fluxzero.home.model.SceneId;
import io.fluxzero.home.model.ScenePlan;
import io.fluxzero.sdk.common.Message;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

import static io.fluxzero.home.model.Rules.require;

/** Describe when a scene should react, with an optional quiet interval between activations. */
public record DefineAutomation(AutomationId automationId, HomeId homeId, @NotNull @Valid AutomationDetails details, SceneId sceneId,
                               AutomationTrigger trigger, Duration cooldown) {
    @AssertLegal void validate(Graph<Home> home, @Nullable Automation automation) {
        require(trigger != null && cooldown != null && !cooldown.isNegative(), "Choose a trigger and a non-negative cooldown.");
        require(automation == null || automation.homeId().equals(homeId), "An automation cannot move between homes.");
        ScenePlan.find(home, sceneId, Scene.class);
        switch (trigger) {
            case AutomationTrigger.HomeBecomes t -> require(t.mode() != null, "Choose a home mode.");
            case AutomationTrigger.MeasurementCrosses t -> {
                var device = ScenePlan.find(home, t.deviceId(), Device.class).get();
                require(t.measurement() != null && device.measurements().contains(t.measurement()), "Choose a measurement supplied by this device.");
                require(t.direction() != null, "Choose a crossing direction.");
                t.measurement().validate(t.threshold());
            }
        }
    }
    @Apply Automation apply(@Nullable Automation automation, Message message) {
        return new Automation(automationId, homeId, details, sceneId, trigger, cooldown, true, message.getTimestamp(),
                automation == null ? 0 : automation.executionCount(), automation == null ? null : automation.lastExecutedAt(), null);
    }
}
