package io.fluxzero.home.automation.api;

import io.fluxzero.home.automation.api.model.Automation;
import io.fluxzero.home.automation.api.model.AutomationDetails;
import io.fluxzero.home.automation.api.model.AutomationTrigger;
import io.fluxzero.home.household.api.HomeId;
import io.fluxzero.home.household.api.model.Home;
import io.fluxzero.home.scenes.api.SceneId;
import io.fluxzero.home.scenes.api.model.Scene;
import io.fluxzero.sdk.common.Message;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

/** Describe when a scene should react, with an optional quiet interval between activations. */
public record DefineAutomation(AutomationId automationId, HomeId homeId, @NotNull @Valid AutomationDetails details,
                               @NotNull SceneId sceneId,
                               @NotNull @Valid @AssertLegal AutomationTrigger trigger, @NotNull Duration cooldown) {
    @AssertTrue(message = "Choose a non-negative cooldown.")
    boolean hasNonNegativeCooldown() {
        return !cooldown.isNegative();
    }

    @AssertLegal
    void remainsInHome(@Nullable Automation automation) {
        if (automation != null && !automation.homeId().equals(homeId)) {
            throw new IllegalCommandException("An automation cannot move between homes.");
        }
    }

    @AssertLegal
    void sceneBelongsToHome(Graph<Home> home) {
        if (home.find(sceneId, Scene.class).isEmpty()) {
            throw new IllegalCommandException("Choose an existing scene from this home.");
        }
    }

    @Apply
    Automation apply(@Nullable Automation automation, Message message) {
        var cooldownEndsAt = automation == null || automation.cooldownEndsAt() == null ? null
                : automation.cooldownEndsAt().minus(automation.cooldown()).plus(cooldown);
        return new Automation(automationId, homeId, details, sceneId, trigger, cooldown, true, message.getTimestamp(),
                cooldownEndsAt, null);
    }
}
