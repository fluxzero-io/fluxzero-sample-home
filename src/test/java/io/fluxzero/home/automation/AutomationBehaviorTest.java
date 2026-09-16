package io.fluxzero.home.automation;

import io.fluxzero.home.automation.api.DefineAutomation;
import io.fluxzero.home.automation.api.PauseAutomation;
import io.fluxzero.home.automation.api.ReactToHome;
import io.fluxzero.home.automation.api.ResumeAutomation;
import io.fluxzero.home.automation.api.model.AutomationDetails;
import io.fluxzero.home.automation.api.model.DeviceObservationChanged;
import io.fluxzero.home.automation.api.model.HomeBecomes;
import io.fluxzero.home.automation.api.model.HomeModeChanged;
import io.fluxzero.home.automation.api.model.MeasurementCrosses;
import io.fluxzero.home.devices.api.AddDevice;
import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.devices.api.RemoveDevice;
import io.fluxzero.home.devices.api.ReportDeviceStatus;
import io.fluxzero.home.devices.api.model.Availability;
import io.fluxzero.home.devices.api.model.DeviceDetails;
import io.fluxzero.home.devices.api.model.DeviceSettings;
import io.fluxzero.home.devices.api.model.DeviceStatus;
import io.fluxzero.home.devices.api.model.Measurement;
import io.fluxzero.home.household.api.ChangeHomeMode;
import io.fluxzero.home.household.api.CreateHome;
import io.fluxzero.home.household.api.HomeId;
import io.fluxzero.home.household.api.RenameHome;
import io.fluxzero.home.household.api.model.HomeDetails;
import io.fluxzero.home.household.api.model.HomeMode;
import io.fluxzero.home.scenes.api.ActivateScene;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.tracking.handling.HandleEvent;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static io.fluxzero.home.HouseExample.*;
import static org.junit.jupiter.api.Assertions.*;

class AutomationBehaviorTest {
    DefineAutomation onAway() {
        return new DefineAutomation(REACTION, HOME, new AutomationDetails("Leaving home"), EVENING, new HomeBecomes(HomeMode.AWAY), Duration.ZERO);
    }
    DefineAutomation onHeat() {
        return new DefineAutomation(REACTION, HOME, new AutomationDetails("Too warm"), EVENING, new MeasurementCrosses(SENSOR, Measurement.TEMPERATURE,
                        MeasurementCrosses.Direction.RISES_ABOVE, new BigDecimal("24")), Duration.ofMinutes(5));
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void modeChangeActivatesSceneAndRepeatedModeDoesNot(boolean async) {
        (async ? asyncHouse(new HomeReactions()) : house(new HomeReactions())).givenCommands(evening(), onAway())
                .whenCommand(new ChangeHomeMode(HOME, HomeMode.AWAY)).expectNoErrors()
                .expectEvents(new ActivateScene(EVENING))
                .expectThat(f -> assertEvening())
                .andThen().whenCommand(new ChangeHomeMode(HOME, HomeMode.AWAY)).expectNoErrors()
                .expectNoEventsLike(ActivateScene.class);
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void crossingNeedsPreviousEvidenceAndHonorsCooldown(boolean async) {
        (async ? asyncHouse(new HomeReactions()) : house(new HomeReactions())).givenCommands(evening(), onHeat())
                .whenCommand(temperature(NOW, "25")).expectNoErrors()
                .expectNoEventsLike(ActivateScene.class)
                .expectThat(f -> assertNull(Fluxzero.loadModel(REACTION).get().cooldownEndsAt()))
                .andThen().whenTimeElapses(Duration.ofSeconds(1)).expectNoErrors()
                .andThen().whenCommand(temperature(NOW.plusSeconds(1), "23")).expectNoErrors()
                .expectThat(f -> assertEquals(new BigDecimal("23"), Fluxzero.loadModel(SENSOR, DeviceStatus.class).get().readings().get(Measurement.TEMPERATURE)))
                .andThen().whenTimeElapses(Duration.ofSeconds(1)).expectNoErrors()
                .andThen().whenCommand(temperature(NOW.plusSeconds(2), "25")).expectNoErrors()
                .expectEvents(new ActivateScene(EVENING))
                .expectThat(f -> assertEquals(NOW.plusSeconds(302), Fluxzero.loadModel(REACTION).get().cooldownEndsAt()))
                .andThen().whenTimeElapses(Duration.ofSeconds(1)).expectNoErrors()
                .andThen().whenCommand(temperature(NOW.plusSeconds(3), "23")).expectNoErrors()
                .andThen().whenTimeElapses(Duration.ofSeconds(1)).expectNoErrors()
                .andThen().whenCommand(temperature(NOW.plusSeconds(4), "25")).expectNoErrors()
                .expectNoEventsLike(ActivateScene.class)
                .expectThat(f -> assertEquals(NOW.plusSeconds(302), Fluxzero.loadModel(REACTION).get().cooldownEndsAt()));
    }
    @Test void pausedAutomationDoesNotReact() {
        house(new HomeReactions()).givenCommands(evening(), onAway(), new PauseAutomation(REACTION))
                .whenCommand(new ChangeHomeMode(HOME, HomeMode.AWAY)).expectNoErrors()
                .expectNoEventsLike(ActivateScene.class)
                .expectThat(f -> assertNull(Fluxzero.loadModel(REACTION).get().cooldownEndsAt()));
    }
    @Test void otherHomesDoNotTriggerThisHousehold() {
        var other = new HomeId("other");
        house(new HomeReactions()).givenCommands(evening(), onAway(), new CreateHome(other, new HomeDetails("Other"), AMSTERDAM))
                .whenCommand(new ChangeHomeMode(other, HomeMode.AWAY)).expectNoErrors()
                .expectNoEventsLike(ActivateScene.class)
                .expectThat(f -> assertNull(Fluxzero.loadModel(REACTION).get().cooldownEndsAt()));
    }

    @Test void unavailableScenePausesAutomationWithoutPartlyChangingDevices() {
        house(new HomeReactions()).givenCommands(evening(), onAway(), new RemoveDevice(HEAT))
                .whenCommand(new ChangeHomeMode(HOME, HomeMode.AWAY)).expectNoErrors().expectThat(f -> {
                    var automation = Fluxzero.loadModel(REACTION).get();
                    assertFalse(automation.enabled()); assertNotNull(automation.problem());
                    assertTrue(Fluxzero.loadModel(LIGHT).get().desiredSettings().isEmpty());
                });
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void observationHistorySurvivesCacheClear(boolean async) {
        (async ? asyncHouse() : house()).givenCommands(temperature(NOW.minusSeconds(1), "23"))
                .whenCommand(temperature(NOW, "25")).expectNoErrors().expectThat(f -> {
                    f.cache().clear();
                    var status = Fluxzero.loadGraph(SENSOR, DeviceStatus.class);
                    assertEquals(new BigDecimal("23"), status.previous().get().readings().get(Measurement.TEMPERATURE));
                    assertEquals(new BigDecimal("25"), status.get().readings().get(Measurement.TEMPERATURE));
                });
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void olderObservationKeepsItsOwnBeforeAndAfter(boolean async) {
        var crossing = new AtomicReference<Graph<DeviceStatus>>();
        var observer = new Object() {
            @HandleEvent
            void observed(ReportDeviceStatus event, Graph<DeviceStatus> graph) {
                if (event.observedAt().equals(NOW.minusSeconds(1))) crossing.set(graph);
            }
        };
        (async ? asyncHouse(observer) : house(observer))
                .givenCommands(temperature(NOW.minusSeconds(2), "23"), temperature(NOW.minusSeconds(1), "25"))
                .whenCommand(temperature(NOW, "22")).expectNoErrors().expectThat(f -> {
                    f.cache().clear();
                    assertEquals(new BigDecimal("23"), crossing.get().previous().get().readings().get(Measurement.TEMPERATURE));
                    assertEquals(new BigDecimal("25"), crossing.get().get().readings().get(Measurement.TEMPERATURE));
                    assertEquals(new BigDecimal("22"), Fluxzero.loadModel(SENSOR, DeviceStatus.class).get().readings().get(Measurement.TEMPERATURE));
                });
    }
    @Test void resumingWaitsForTheNextModeChange() {
        house(new HomeReactions()).givenCommands(evening(), onAway(), new PauseAutomation(REACTION),
                new ChangeHomeMode(HOME, HomeMode.AWAY))
                .whenCommand(new ResumeAutomation(REACTION)).expectNoErrors()
                .expectNoEventsLike(ActivateScene.class)
                .expectThat(f -> assertNull(Fluxzero.loadModel(REACTION).get().cooldownEndsAt()))
                .andThen().whenCommand(new ChangeHomeMode(HOME, HomeMode.HOME)).expectNoErrors()
                .andThen().whenCommand(new ChangeHomeMode(HOME, HomeMode.AWAY)).expectNoErrors()
                .expectEvents(new ActivateScene(EVENING));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void renamingTheHomeDoesNotDiscardItsDeparture(boolean async) {
        var departure = new HomeModeChanged(NOW, HomeMode.HOME, HomeMode.AWAY);
        (async ? asyncHouse() : house()).givenCommands(evening(), onAway(), new ChangeHomeMode(HOME, HomeMode.AWAY),
                        new RenameHome(HOME, "Canal home"))
                .whenCommand(new ReactToHome(REACTION, departure)).expectNoErrors()
                .expectEvents(new ActivateScene(EVENING))
                .expectThat(f -> assertEvening());
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void aLaterReadingAboveTheThresholdDoesNotDiscardTheCrossing(boolean async) {
        var crossing = new DeviceObservationChanged(SENSOR, NOW,
                Map.of(Measurement.TEMPERATURE, new BigDecimal("23")),
                Map.of(Measurement.TEMPERATURE, new BigDecimal("25")));
        (async ? asyncHouse() : house()).givenCommands(evening(), onHeat(),
                        temperature(NOW.minusSeconds(2), "23"), temperature(NOW.minusSeconds(1), "25"), temperature(NOW, "26"))
                .whenCommand(new ReactToHome(REACTION, crossing)).expectNoErrors()
                .expectEvents(new ActivateScene(EVENING))
                .expectThat(f -> assertEvening());
    }

    @Test void downwardCrossingCanActivateAScene() {
        var trigger = new MeasurementCrosses(SENSOR, Measurement.TEMPERATURE,
                MeasurementCrosses.Direction.FALLS_BELOW, new BigDecimal("18"));
        house(new HomeReactions()).givenCommands(evening(), new DefineAutomation(REACTION, HOME, new AutomationDetails("Cold"), EVENING, trigger, Duration.ZERO),
                temperature(NOW.minusSeconds(1), "19"))
                .whenCommand(temperature(NOW, "17")).expectNoErrors()
                .expectEvents(new ActivateScene(EVENING));
    }

    @ParameterizedTest
    @CsvSource({
            "RISES_ABOVE, 24, 25, 1",
            "RISES_ABOVE, 23, 24, 0",
            "RISES_ABOVE, 25, 26, 0",
            "RISES_ABOVE, 25, 23, 0",
            "FALLS_BELOW, 24, 23, 1",
            "FALLS_BELOW, 25, 24, 0",
            "FALLS_BELOW, 23, 22, 0",
            "FALLS_BELOW, 23, 25, 0"
    })
    void aReadingMustPassTheThresholdInTheChosenDirection(MeasurementCrosses.Direction direction,
                                                         String before, String after, long activations) {
        var trigger = new MeasurementCrosses(SENSOR, Measurement.TEMPERATURE, direction, new BigDecimal("24"));
        house(new HomeReactions()).givenCommands(evening(),
                        new DefineAutomation(REACTION, HOME, new AutomationDetails("Threshold"), EVENING, trigger, Duration.ZERO),
                        temperature(NOW.minusSeconds(1), before))
                .whenCommand(temperature(NOW, after)).expectNoErrors()
                .expectThat(f -> assertEquals(activations > 0, Fluxzero.loadModel(REACTION).get().cooldownEndsAt() != null));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void aMissingReadingBreaksTheEvidenceForACrossing(boolean async) {
        (async ? asyncHouse(new HomeReactions()) : house(new HomeReactions()))
                .givenCommands(evening(), onHeat(), temperature(NOW.minusSeconds(2), "23"))
                .whenCommand(new ReportDeviceStatus(SENSOR,
                        NOW.minusSeconds(1), Availability.ONLINE, DeviceSettings.empty(), Map.of()))
                .expectNoErrors()
                .expectNoEventsLike(ActivateScene.class)
                .expectThat(f -> assertNull(Fluxzero.loadModel(REACTION).get().cooldownEndsAt()))
                .andThen().whenCommand(temperature(NOW, "25")).expectNoErrors()
                .expectNoEventsLike(ActivateScene.class)
                .expectThat(f -> assertNull(Fluxzero.loadModel(REACTION).get().cooldownEndsAt()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void onlyTheChosenSensorCanTriggerTheAutomation(boolean async) {
        var otherSensor = new DeviceId("garden-sensor");
        (async ? asyncHouse(new HomeReactions()) : house(new HomeReactions()))
                .givenCommands(evening(), onHeat(),
                        new AddDevice(otherSensor, GARDEN, new DeviceDetails("Garden sensor"), null,
                                Set.of(), Set.of(Measurement.TEMPERATURE)),
                        new ReportDeviceStatus(otherSensor,
                                NOW.minusSeconds(1), Availability.ONLINE, DeviceSettings.empty(),
                                Map.of(Measurement.TEMPERATURE, new BigDecimal("23"))))
                .whenCommand(new ReportDeviceStatus(otherSensor,
                        NOW, Availability.ONLINE, DeviceSettings.empty(),
                        Map.of(Measurement.TEMPERATURE, new BigDecimal("25"))))
                .expectNoErrors()
                .expectNoEventsLike(ActivateScene.class)
                .expectThat(f -> assertNull(Fluxzero.loadModel(REACTION).get().cooldownEndsAt()));
    }

    @Test
    void changingTheTriggerBeforeAReactionUsesTheNewDefinition() {
        var departure = new HomeModeChanged(NOW, HomeMode.HOME, HomeMode.AWAY);
        house().givenCommands(evening(), onAway(),
                        new DefineAutomation(REACTION, HOME, new AutomationDetails("On return"), EVENING,
                                new HomeBecomes(HomeMode.HOME), Duration.ZERO))
                .whenCommand(new ReactToHome(REACTION, departure)).expectNoErrors()
                .expectNoEventsLike(ActivateScene.class)
                .expectThat(f -> assertNull(Fluxzero.loadModel(REACTION).get().cooldownEndsAt()));
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void cooldownEndsExactlyAtItsDeadlineAndSurvivesPauseAndResume(boolean async) {
        var definition = new DefineAutomation(REACTION, HOME, new AutomationDetails("Leaving home"),
                EVENING, new HomeBecomes(HomeMode.AWAY), Duration.ofMinutes(5));
        (async ? asyncHouse(new HomeReactions()) : house(new HomeReactions()))
                .givenCommands(evening(), definition, new ChangeHomeMode(HOME, HomeMode.AWAY))
                .whenCommand(new PauseAutomation(REACTION)).expectNoErrors()
                .andThen().whenCommand(new ResumeAutomation(REACTION)).expectNoEventsLike(ActivateScene.class)
                .andThen().givenCommands(new ChangeHomeMode(HOME, HomeMode.HOME))
                .whenTimeAdvancesTo(NOW.plusSeconds(300).minusNanos(1)).expectNoErrors()
                .andThen().whenCommand(new ChangeHomeMode(HOME, HomeMode.AWAY))
                .expectNoEventsLike(ActivateScene.class)
                .andThen().givenCommands(new ChangeHomeMode(HOME, HomeMode.HOME))
                .whenTimeAdvancesTo(NOW.plusSeconds(300)).expectNoErrors()
                .andThen().whenCommand(new ChangeHomeMode(HOME, HomeMode.AWAY))
                .expectEvents(new ActivateScene(EVENING)).expectNoErrors()
                .expectThat(f -> assertEquals(NOW.plusSeconds(600), Fluxzero.loadModel(REACTION).get().cooldownEndsAt()));
    }

    @Test
    void zeroCooldownAllowsAnotherDistinctDepartureAtTheSameTime() {
        house(new HomeReactions()).givenCommands(evening(), onAway(),
                        new ChangeHomeMode(HOME, HomeMode.AWAY), new ChangeHomeMode(HOME, HomeMode.HOME))
                .whenCommand(new ChangeHomeMode(HOME, HomeMode.AWAY)).expectNoErrors()
                .expectOnlyEvents(new ChangeHomeMode(HOME, HomeMode.AWAY), new ActivateScene(EVENING),
                        new ReactToHome(REACTION, new HomeModeChanged(NOW, HomeMode.HOME, HomeMode.AWAY)));
    }

    @ParameterizedTest @ValueSource(longs = {0, 60, 600})
    void changingCooldownKeepsItsOriginAtTheLastSuccessfulExecution(long seconds) {
        house(new HomeReactions()).givenCommands(evening(), onAway(), new ChangeHomeMode(HOME, HomeMode.AWAY))
                .atFixedTime(NOW.plusSeconds(30))
                .whenCommand(new DefineAutomation(REACTION, HOME, new AutomationDetails("Leaving home"), EVENING,
                        new HomeBecomes(HomeMode.AWAY), Duration.ofSeconds(seconds))).expectNoErrors()
                .expectNoEventsLike(ActivateScene.class)
                .expectThat(f -> assertEquals(NOW.plusSeconds(seconds), Fluxzero.loadModel(REACTION).get().cooldownEndsAt()));
    }

    @Test
    void aNewDefinitionDoesNotReactToAnEarlierChange() {
        house().givenCommands(evening(), onAway()).atFixedTime(NOW.plusSeconds(1))
                .givenCommands(onAway())
                .whenCommand(new ReactToHome(REACTION, new HomeModeChanged(NOW, HomeMode.HOME, HomeMode.AWAY)))
                .expectNoErrors().expectNoEvents();
    }

}
