package io.fluxzero.home.scenes;

import io.fluxzero.home.automation.RoutineSchedules;
import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.devices.api.DimLight;
import io.fluxzero.home.devices.api.RemoveDevice;
import io.fluxzero.home.devices.api.SetRoomTemperature;
import io.fluxzero.home.devices.api.model.Capability;
import io.fluxzero.home.devices.api.model.Device;
import io.fluxzero.home.devices.api.model.LightLevel;
import io.fluxzero.home.devices.api.model.Power;
import io.fluxzero.home.devices.api.model.RoomTemperature;
import io.fluxzero.home.household.api.CreateHome;
import io.fluxzero.home.household.api.DefineZone;
import io.fluxzero.home.household.api.HomeId;
import io.fluxzero.home.household.api.ZoneId;
import io.fluxzero.home.household.api.model.Home;
import io.fluxzero.home.household.api.model.HomeDetails;
import io.fluxzero.home.household.api.model.ZoneDetails;
import io.fluxzero.home.scenes.api.ActivateScene;
import io.fluxzero.home.scenes.api.DefineScene;
import io.fluxzero.home.scenes.api.RemoveScene;
import io.fluxzero.home.scenes.api.SceneId;
import io.fluxzero.home.scenes.api.model.DimLights;
import io.fluxzero.home.scenes.api.model.InZone;
import io.fluxzero.home.scenes.api.model.OneDevice;
import io.fluxzero.home.scenes.api.model.SceneDetails;
import io.fluxzero.home.scenes.api.model.SetHeating;
import io.fluxzero.home.scenes.api.model.SwitchPower;
import io.fluxzero.home.scenes.api.model.WholeHome;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.persisting.eventsourcing.InterceptApply;
import io.fluxzero.sdk.tracking.handling.HandleEvent;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;
import io.fluxzero.sdk.tracking.handling.validation.ValidationException;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import static io.fluxzero.home.HouseExample.*;
import static org.junit.jupiter.api.Assertions.*;

class SceneBehaviorTest {
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void oneSceneChangesLightAndHeatingTogether(boolean async) {
        (async ? asyncHouse(new AtomicSceneObserver()) : house(new AtomicSceneObserver())).givenCommands(evening())
                .whenCommand(new ActivateScene(EVENING)).expectEvents(new ActivateScene(EVENING)).expectNoErrors()
                .expectThat(f -> assertEvening());
    }
    @Test void invalidSceneDoesNotPartlyChangeDevices() {
        var invalid = new DefineScene(EVENING, HOME, new SceneDetails("Broken"), List.of(
                new DimLights(new OneDevice(LIGHT), new LightLevel(20)),
                new SetHeating(new OneDevice(LIGHT), new RoomTemperature(new BigDecimal("21")))));
        house().whenCommand(invalid).expectExceptionalResult(IllegalCommandException.class).expectNoEvents()
                .expectThat(f -> {
                    assertTrue(Fluxzero.loadModel(LIGHT).get().pendingSettings().isEmpty());
                    assertNull(Fluxzero.loadModel(EVENING).get());
                });
    }
    @Test void missingSceneTargetsFailAtActivationWithoutPartialChanges() {
        house().givenCommands(evening(), new RemoveDevice(HEAT))
                .whenCommand(new ActivateScene(EVENING)).expectExceptionalResult(IllegalCommandException.class).expectNoEvents()
                .expectThat(f -> assertTrue(Fluxzero.loadModel(LIGHT).get().pendingSettings().isEmpty()));
    }
    @Test void sceneCannotReachIntoAnotherHome() {
        var other = new HomeId("other");
        house().givenCommands(new CreateHome(other, new HomeDetails("Other"), AMSTERDAM))
                .whenCommand(new DefineScene(new SceneId("foreign"), other, new SceneDetails("Foreign"), List.of(
                        new SwitchPower(new OneDevice(LIGHT), new Power(true)))))
                .expectExceptionalResult(IllegalCommandException.class).expectNoEvents();
    }
    @Test void overlappingZoneVisitsEachDeviceOnceAndLastSettingWins() {
        var zone = new ZoneId("downstairs");
        house().givenCommands(new DefineZone(zone, HOME, new ZoneDetails("Downstairs"), Set.of(FLOOR, LIVING)),
                new DefineScene(EVENING, HOME, new SceneDetails("Soft light"), List.of(
                        new DimLights(new InZone(zone), new LightLevel(50)),
                        new DimLights(new OneDevice(LIGHT), new LightLevel(20)))))
                .whenCommand(new ActivateScene(EVENING))
                .expectOnlyEvents(new DimLight(LIGHT, new LightLevel(20)), new ActivateScene(EVENING)).expectNoErrors()
                .expectThat(f -> assertEquals(new LightLevel(20), Fluxzero.loadModel(LIGHT).get().pendingSettings().get(Capability.LIGHT_LEVEL)));
    }
    @Test void aSceneUsedByARoutineCannotBeRemoved() {
        house(new RoutineSchedules()).givenCommands(evening(), once(NOW.plusSeconds(60)))
                .whenCommand(new RemoveScene(EVENING)).expectExceptionalResult(IllegalCommandException.class).expectNoEvents()
                .expectOnlyActiveScheduledCommands(scheduled(1, NOW.plusSeconds(60)));
    }

    @Test void wholeHomeTargetsOnlyDevicesWithTheRequestedCapability() {
        house().givenCommands(new DefineScene(EVENING, HOME, new SceneDetails("Lights out"), List.of(
                new SwitchPower(new WholeHome(), new Power(false)))))
                .whenCommand(new ActivateScene(EVENING)).expectNoErrors().expectThat(f -> {
                    assertEquals(new Power(false), Fluxzero.loadModel(LIGHT).get().pendingSettings().get(Capability.POWER));
                    assertTrue(Fluxzero.loadModel(HEAT).get().pendingSettings().isEmpty());
                });
    }
    @Test void emptySceneIsRejected() {
        house().whenCommand(new DefineScene(EVENING, HOME, new SceneDetails("Empty"), List.of()))
                .expectExceptionalResult(ValidationException.class).expectNoEvents();
    }
    @Test void repeatedActivationKeepsItsHistoryWithoutRepeatingDeviceChanges() {
        house().givenCommands(evening(), new ActivateScene(EVENING))
                .whenCommand(new ActivateScene(EVENING)).expectOnlyEvents(new ActivateScene(EVENING))
                .andThen().whenExecuting(f -> f.cache().clear()).expectNoErrors()
                .expectThat(f -> assertEvening());
    }

    @Test void anOverriddenInvalidActionStillRejectsTheScene() {
        house().whenCommand(new DefineScene(EVENING, HOME, new SceneDetails("Invalid brightness"), List.of(
                        new DimLights(new OneDevice(LIGHT), new LightLevel(101)),
                        new DimLights(new OneDevice(LIGHT), new LightLevel(20)))))
                .expectExceptionalResult(ValidationException.class).expectNoEvents();
    }

    static class AtomicSceneObserver {
        @HandleEvent
        void changed(DimLight event, Graph<Home> home) {
            if (event.brightness().percent() == 25) {
                // Event injection intentionally pins this individual event. Check the complete durable commit
                // through an explicitly current view, as a live reader would see it.
                home = Fluxzero.loadCurrentGraph(home.get().id());
                assertEquals(new RoomTemperature(new BigDecimal("21")),
                        home.find(HEAT, Device.class).orElseThrow().get().pendingSettings().get(Capability.TEMPERATURE));
            }
        }
    }
    record RejectAfterScene(SceneId sceneId, DeviceId deviceId) {
        @InterceptApply
        List<Object> prepare() {
            return List.of(new ActivateScene(sceneId), new SetRoomTemperature(deviceId, new RoomTemperature(new BigDecimal("21"))));
        }
    }

    @Test void aLaterRejectedStepRollsBackTheEntireScene() {
        house().givenCommands(evening()).whenCommand(new RejectAfterScene(EVENING, LIGHT))
                .expectExceptionalResult(IllegalCommandException.class).expectNoEvents().expectThat(f -> {
                    assertTrue(Fluxzero.loadModel(LIGHT).get().pendingSettings().isEmpty());
                    assertTrue(Fluxzero.loadModel(HEAT).get().pendingSettings().isEmpty());
                });
    }
}
