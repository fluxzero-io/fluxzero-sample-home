package io.fluxzero.home;

import io.fluxzero.home.model.*;
import io.fluxzero.home.command.*;
import io.fluxzero.home.query.*;
import io.fluxzero.home.automation.*;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.modeling.*;
import io.fluxzero.sdk.test.*;
import io.fluxzero.sdk.scheduling.Schedule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import static io.fluxzero.home.HouseExample.*;
import static org.junit.jupiter.api.Assertions.*;

class SceneBehaviorTest {
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void oneSceneChangesLightAndHeatingTogether(boolean async) {
        (async ? asyncHouse(new AtomicSceneObserver()) : house(new AtomicSceneObserver())).givenCommands(evening())
                .whenCommand(new ActivateScene(EVENING)).expectEvents(new ActivateScene(EVENING)).expectNoErrors()
                .expectThat(f -> { assertEvening();
                    assertEquals(1, Fluxzero.loadModel(EVENING).get().activationCount()); });
    }
    @Test void invalidSceneDoesNotPartlyChangeDevices() {
        var invalid = new DefineScene(EVENING, HOME, new SceneDetails("Broken"), List.of(
                new SceneAction(new SceneTarget.OneDevice(LIGHT), new DeviceSetting.LightLevel(20)),
                new SceneAction(new SceneTarget.OneDevice(LIGHT), new DeviceSetting.Temperature(new BigDecimal("21")))));
        house().whenCommand(invalid).expectExceptionalResult(HomeRuleViolation.class).expectNoEvents()
                .expectThat(f -> { assertTrue(Fluxzero.loadModel(LIGHT).get().desiredSettings().isEmpty()); assertNull(Fluxzero.loadModel(EVENING).get()); });
    }
    @Test void missingSceneTargetsFailAtActivationWithoutPartialChanges() {
        house().givenCommands(evening(), new RemoveDevice(HEAT))
                .whenCommand(new ActivateScene(EVENING)).expectExceptionalResult(HomeRuleViolation.class).expectNoEvents()
                .expectThat(f -> { assertTrue(Fluxzero.loadModel(LIGHT).get().desiredSettings().isEmpty()); assertEquals(0, Fluxzero.loadModel(EVENING).get().activationCount()); });
    }
    @Test void sceneCannotReachIntoAnotherHome() {
        var other = new HomeId("other");
        house().givenCommands(new CreateHome(other, new HomeDetails("Other"), AMSTERDAM))
                .whenCommand(new DefineScene(new SceneId("foreign"), other, new SceneDetails("Foreign"), List.of(
                        new SceneAction(new SceneTarget.OneDevice(LIGHT), new DeviceSetting.Power(true)))))
                .expectExceptionalResult(HomeRuleViolation.class).expectNoEvents();
    }
    @Test void overlappingZoneVisitsEachDeviceOnceAndLastSettingWins() {
        var zone = new io.fluxzero.home.model.ZoneId("downstairs");
        house().givenCommands(new DefineZone(zone, HOME, new ZoneDetails("Downstairs"), Set.of(FLOOR, LIVING)),
                new DefineScene(EVENING, HOME, new SceneDetails("Soft light"), List.of(
                        new SceneAction(new SceneTarget.InZone(zone), new DeviceSetting.LightLevel(50)),
                        new SceneAction(new SceneTarget.OneDevice(LIGHT), new DeviceSetting.LightLevel(20)))))
                .whenCommand(new ActivateScene(EVENING)).expectEvents(new ActivateScene(EVENING)).expectNoErrors()
                .expectThat(f -> assertEquals(new DeviceSetting.LightLevel(20), Fluxzero.loadModel(LIGHT).get().desiredSettings().get(Capability.LIGHT_LEVEL)));
    }
    @Test void aSceneUsedByARoutineCannotBeRemoved() {
        house(new RoutineSchedules()).givenCommands(evening(), once(NOW.plusSeconds(60)))
                .whenCommand(new RemoveScene(EVENING)).expectExceptionalResult(HomeRuleViolation.class).expectNoEvents()
                .expectOnlySchedules(scheduled(1, NOW.plusSeconds(60)));
    }

    @Test void wholeHomeTargetsOnlyDevicesWithTheRequestedCapability() {
        house().givenCommands(new DefineScene(EVENING, HOME, new SceneDetails("Lights out"), List.of(
                new SceneAction(new SceneTarget.WholeHome(), new DeviceSetting.Power(false)))))
                .whenCommand(new ActivateScene(EVENING)).expectNoErrors().expectThat(f -> {
                    assertEquals(new DeviceSetting.Power(false), Fluxzero.loadModel(LIGHT).get().desiredSettings().get(Capability.POWER));
                    assertTrue(Fluxzero.loadModel(HEAT).get().desiredSettings().isEmpty());
                });
    }
    @Test void emptySceneIsRejected() {
        house().whenCommand(new DefineScene(EVENING, HOME, new SceneDetails("Empty"), List.of()))
                .expectExceptionalResult(HomeRuleViolation.class).expectNoEvents();
    }
    static class AtomicSceneObserver {
        @io.fluxzero.sdk.tracking.handling.HandleEvent
        void changed(DimLight event, Graph<Home> home) {
            if (event.percent() == 25) {
                // Event injection intentionally pins this individual event. Check the complete durable commit
                // through an explicitly current view, as a live reader would see it.
                home = Fluxzero.loadCurrentGraph(home.get().homeId());
                assertEquals(new DeviceSetting.Temperature(new BigDecimal("21")),
                        home.find(HEAT, Device.class).orElseThrow().get().desiredSettings().get(Capability.TEMPERATURE));
                assertEquals(1, home.find(EVENING, Scene.class).orElseThrow().get().activationCount());
            }
        }
    }
    record RejectAfterScene(SceneId sceneId, DeviceId deviceId) {
        @io.fluxzero.sdk.persisting.eventsourcing.InterceptApply
        List<Object> prepare() {
            return List.of(new ActivateScene(sceneId), new SetRoomTemperature(deviceId, new BigDecimal("21")));
        }
    }

    @Test void aLaterRejectedStepRollsBackTheEntireScene() {
        house().givenCommands(evening()).whenCommand(new RejectAfterScene(EVENING, LIGHT))
                .expectExceptionalResult(HomeRuleViolation.class).expectNoEvents().expectThat(f -> {
                    assertTrue(Fluxzero.loadModel(LIGHT).get().desiredSettings().isEmpty());
                    assertTrue(Fluxzero.loadModel(HEAT).get().desiredSettings().isEmpty());
                    assertEquals(0, Fluxzero.loadModel(EVENING).get().activationCount());
                });
    }
}
