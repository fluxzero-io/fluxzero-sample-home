package io.fluxzero.home;

import io.fluxzero.home.command.DimLight;
import io.fluxzero.home.command.SetRoomTemperature;
import io.fluxzero.home.model.Device;
import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.Scene;
import io.fluxzero.home.model.SceneId;
import io.fluxzero.home.model.ScenePlan;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.persisting.eventsourcing.Apply;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;

import static io.fluxzero.home.HouseExample.*;

/** Qualifies the former dynamic-write conflict using Home's real selection and device Models. */
class DynamicSceneContractTest {
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void selectedDevicesRetainTheirDifferentRevisionsAndReplay(boolean async) {
        (async ? asyncHouse() : house()).givenCommands(evening(), new DimLight(LIGHT, 50),
                        new DimLight(LIGHT, 60), new SetRoomTemperature(HEAT, new BigDecimal("19")))
                .whenCommand(new ApplySelectedSceneSettings(EVENING)).expectNoErrors()
                .expectOnlyEvents(new ApplySelectedSceneSettings(EVENING))
                .expectThat(f -> {
                    assertEvening();
                    f.cache().clear();
                    assertEvening();
                });
    }

    // Production scenes keep their ordinary device commands; this isolates the alternative SDK contract.
    record ApplySelectedSceneSettings(SceneId sceneId) {
        @Apply List<Device> apply(Scene scene, Graph<Home> home) {
            return ScenePlan.devices(scene.actions(), home);
        }
    }
}
