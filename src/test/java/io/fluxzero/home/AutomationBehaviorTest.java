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

class AutomationBehaviorTest {
    DefineAutomation onAway() {
        return new DefineAutomation(REACTION, HOME, "Leaving home", EVENING,
                new AutomationTrigger.HomeBecomes(HomeMode.AWAY), Duration.ZERO);
    }
    DefineAutomation onHeat() {
        return new DefineAutomation(REACTION, HOME, "Too warm", EVENING,
                new AutomationTrigger.MeasurementCrosses(SENSOR, Measurement.TEMPERATURE,
                        AutomationTrigger.Direction.RISES_ABOVE, new BigDecimal("24")), Duration.ofMinutes(5));
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void modeChangeActivatesSceneAndRepeatedModeDoesNot(boolean async) {
        (async ? asyncHouse(new HomeReactions()) : house(new HomeReactions())).givenCommands(evening(), onAway())
                .whenCommand(new ChangeHomeMode(HOME, HomeMode.AWAY)).expectNoErrors()
                .expectThat(f -> { assertEvening(); assertEquals(1, Fluxzero.loadModel(REACTION).get().executionCount()); })
                .andThen().whenCommand(new ChangeHomeMode(HOME, HomeMode.AWAY)).expectNoErrors()
                .expectThat(f -> assertEquals(1, Fluxzero.loadModel(REACTION).get().executionCount()));
    }
    @Test void crossingNeedsPreviousEvidenceAndHonorsCooldown() {
        house(new HomeReactions()).givenCommands(evening(), onHeat())
                .whenCommand(temperature(NOW, "25")).expectNoErrors()
                .expectThat(f -> assertEquals(0, Fluxzero.loadModel(REACTION).get().executionCount()))
                .andThen().whenTimeElapses(Duration.ofSeconds(1)).expectNoErrors()
                .andThen().whenCommand(temperature(NOW.plusSeconds(1), "23")).expectNoErrors()
                .expectThat(f -> assertEquals(new BigDecimal("23"), Fluxzero.loadModel(new DeviceStatusId(SENSOR.getFunctionalId())).get().readings().get(Measurement.TEMPERATURE)))
                .andThen().whenTimeElapses(Duration.ofSeconds(1)).expectNoErrors()
                .andThen().whenCommand(temperature(NOW.plusSeconds(2), "25")).expectNoErrors()
                .expectThat(f -> assertEquals(1, Fluxzero.loadModel(REACTION).get().executionCount()))
                .andThen().whenTimeElapses(Duration.ofSeconds(1)).expectNoErrors()
                .andThen().whenCommand(temperature(NOW.plusSeconds(3), "23")).expectNoErrors()
                .andThen().whenTimeElapses(Duration.ofSeconds(1)).expectNoErrors()
                .andThen().whenCommand(temperature(NOW.plusSeconds(4), "25")).expectNoErrors()
                .expectThat(f -> assertEquals(1, Fluxzero.loadModel(REACTION).get().executionCount()));
    }
    @Test void pausedAutomationDoesNotReact() {
        house(new HomeReactions()).givenCommands(evening(), onAway(), new PauseAutomation(REACTION))
                .whenCommand(new ChangeHomeMode(HOME, HomeMode.AWAY)).expectNoErrors()
                .expectThat(f -> assertEquals(0, Fluxzero.loadModel(REACTION).get().executionCount()));
    }
    @Test void otherHomesDoNotTriggerThisHousehold() {
        var other = new HomeId("other");
        house(new HomeReactions()).givenCommands(evening(), onAway(), new CreateHome(other, "Other", AMSTERDAM))
                .whenCommand(new ChangeHomeMode(other, HomeMode.AWAY)).expectNoErrors()
                .expectThat(f -> assertEquals(0, Fluxzero.loadModel(REACTION).get().executionCount()));
    }

    @Test void unavailableScenePausesAutomationWithoutPartlyChangingDevices() {
        house(new HomeReactions()).givenCommands(evening(), onAway(), new RemoveDevice(HEAT))
                .whenCommand(new ChangeHomeMode(HOME, HomeMode.AWAY)).expectNoErrors().expectThat(f -> {
                    var automation = Fluxzero.loadModel(REACTION).get();
                    assertFalse(automation.enabled()); assertNotNull(automation.problem());
                    assertTrue(Fluxzero.loadModel(LIGHT).get().desiredSettings().isEmpty());
                });
    }
    @Test void latestObservationCarriesThePreviousReadingForDurableCrossingDetection() {
        house().givenCommands(temperature(NOW.minusSeconds(1), "23"))
                .whenCommand(temperature(NOW, "25")).expectNoErrors().expectThat(f -> {
                    f.cache().clear();
                    var status = Fluxzero.loadModel(new DeviceStatusId(SENSOR.getFunctionalId())).get();
                    assertEquals(new BigDecimal("23"), status.previousReadings().get(Measurement.TEMPERATURE));
                    assertEquals(new BigDecimal("25"), status.readings().get(Measurement.TEMPERATURE));
                });
    }
    @Test void resumingDoesNotRetroactivelyReactToAChangeWhilePaused() {
        house(new HomeReactions()).givenCommands(evening(), onAway(), new PauseAutomation(REACTION),
                new ChangeHomeMode(HOME, HomeMode.AWAY))
                .whenCommand(new ResumeAutomation(REACTION)).expectNoErrors()
                .andThen().whenEvent(new ChangeHomeMode(HOME, HomeMode.AWAY)).expectNoErrors()
                .expectThat(f -> assertEquals(0, Fluxzero.loadModel(REACTION).get().executionCount()))
                .andThen().whenCommand(new ChangeHomeMode(HOME, HomeMode.HOME)).expectNoErrors()
                .andThen().whenCommand(new ChangeHomeMode(HOME, HomeMode.AWAY)).expectNoErrors()
                .expectThat(f -> assertEquals(1, Fluxzero.loadModel(REACTION).get().executionCount()));
    }

    @Test void duplicateSourceEventCannotRepeatAnActivation() {
        house(new HomeReactions()).givenCommands(evening(), onAway(), new ChangeHomeMode(HOME, HomeMode.AWAY))
                .whenEvent(new ChangeHomeMode(HOME, HomeMode.AWAY)).expectNoErrors()
                .expectThat(f -> assertEquals(1, Fluxzero.loadModel(REACTION).get().executionCount()));
    }

    @Test void downwardCrossingCanActivateAScene() {
        var trigger = new AutomationTrigger.MeasurementCrosses(SENSOR, Measurement.TEMPERATURE,
                AutomationTrigger.Direction.FALLS_BELOW, new BigDecimal("18"));
        house(new HomeReactions()).givenCommands(evening(), new DefineAutomation(REACTION, HOME, "Cold", EVENING, trigger, Duration.ZERO),
                temperature(NOW.minusSeconds(1), "19"))
                .whenCommand(temperature(NOW, "17")).expectNoErrors()
                .expectThat(f -> assertEquals(1, Fluxzero.loadModel(REACTION).get().executionCount()));
    }
}
