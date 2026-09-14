package io.fluxzero.home.command;

import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.home.model.Scene;
import io.fluxzero.home.model.SceneAction;
import io.fluxzero.home.model.SceneDetails;
import io.fluxzero.home.model.SceneId;
import io.fluxzero.sdk.modeling.AssertLegal;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Define the ordered household intentions after checking their inputs and selection. */
public record DefineScene(SceneId sceneId, HomeId homeId, @NotNull @Valid SceneDetails details,
                          @NotEmpty @Valid List<@NotNull SceneAction> actions) {
    public DefineScene {
        actions = actions == null ? null : actions.stream().toList();
    }

    @AssertLegal
    void remainsInHome(@Nullable Scene scene) {
        if (scene != null && !scene.homeId().equals(homeId)) {
            throw new IllegalCommandException("A scene cannot move between homes.");
        }
    }

    @AssertLegal
    void targetsAreAvailable(Graph<Home> home) {
        actions.forEach(action -> action.target().select(home, action.capability()));
    }

    @Apply
    Scene apply(@Nullable Scene scene, Home home) {
        return new Scene(sceneId, homeId, details, actions);
    }
}
