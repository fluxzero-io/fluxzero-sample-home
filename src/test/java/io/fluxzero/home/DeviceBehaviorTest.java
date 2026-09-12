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

class DeviceBehaviorTest {
    static Stream<DeviceCommand> intentions() {
        return Stream.of(new TurnOn(LIGHT), new TurnOff(LIGHT), new DimLight(LIGHT, 42), new SetLightColor(LIGHT, 120, 70),
                new SetRoomTemperature(LIGHT, new BigDecimal("20.5")), new SetOpening(LIGHT, 30), new LockDoor(LIGHT),
                new UnlockDoor(LIGHT), new PlayMedia(LIGHT, "Evening jazz"), new StopMedia(LIGHT), new SetVolume(LIGHT, 30),
                new SetFanSpeed(LIGHT, 60), new StartWatering(LIGHT), new StopWatering(LIGHT), new EnableCharging(LIGHT), new PauseCharging(LIGHT));
    }
    @ParameterizedTest @MethodSource("intentions")
    void everydayCommandsKeepIntentSeparateFromObservation(DeviceCommand command) {
        TestFixture.create().givenCommands(new CreateHome(HOME, "Home", AMSTERDAM),
                new AddSpace(LIVING, HOME, null, "Living", SpaceKind.ROOM),
                new AddDevice(LIGHT, LIVING, "Multifunction device", null, EnumSet.allOf(Capability.class), Set.of()))
                .whenCommand(command).expectOnlyEvents(command).expectThat(f -> {
                    assertEquals(command.setting(), Fluxzero.loadModel(LIGHT).get().desiredSettings().get(command.setting().capability()));
                    assertNull(Fluxzero.loadModel(new DeviceStatusId(LIGHT.getFunctionalId())).get());
                });
    }
    @ParameterizedTest @ValueSource(ints = {-1, 101})
    void invalidBrightnessLeavesDeviceUntouched(int percent) {
        house().whenCommand(new DimLight(LIGHT, percent)).expectExceptionalResult(HomeRuleViolation.class)
                .expectNoEvents().expectThat(f -> assertTrue(Fluxzero.loadModel(LIGHT).get().desiredSettings().isEmpty()));
    }
    @Test void aLightCannotSetRoomTemperature() {
        house().whenCommand(new SetRoomTemperature(LIGHT, new BigDecimal("21")))
                .expectExceptionalResult(HomeRuleViolation.class).expectNoEvents();
    }
    @Test void aNewerObservationWinsAndDesiredSettingsRemainIndependent() {
        house().givenCommands(temperature(NOW.minusSeconds(10), "19"))
                .whenCommand(temperature(NOW, "20")).expectNoErrors()
                .andThen().whenCommand(temperature(NOW.minusSeconds(5), "18"))
                .expectNoEvents().expectThat(f -> {
                    assertEquals(new BigDecimal("20"), Fluxzero.loadModel(new DeviceStatusId(SENSOR.getFunctionalId())).get().readings().get(Measurement.TEMPERATURE));
                    assertTrue(Fluxzero.loadModel(SENSOR).get().desiredSettings().isEmpty());
                });
    }
    @Test void aDuplicateObservationIsSuppressed() {
        house().givenCommands(temperature(NOW, "20")).whenCommand(temperature(NOW, "19"))
                .expectNoEvents().expectThat(f -> assertEquals(new BigDecimal("20"),
                        Fluxzero.loadModel(new DeviceStatusId(SENSOR.getFunctionalId())).get().readings().get(Measurement.TEMPERATURE)));
    }
    @Test void futureObservationsAreRejected() {
        house().whenCommand(temperature(NOW.plusSeconds(1), "20")).expectExceptionalResult(HomeRuleViolation.class).expectNoEvents();
    }
    @Test void unsupportedMeasurementsAreRejected() {
        house().whenCommand(new ReportDeviceStatus(new DeviceStatusId(SENSOR.getFunctionalId()), SENSOR, NOW, Availability.ONLINE,
                Map.of(), Map.of(Measurement.HUMIDITY, new BigDecimal("50"))))
                .expectExceptionalResult(HomeRuleViolation.class).expectNoEvents();
    }

    @ParameterizedTest @ValueSource(strings = {"4.9", "35.1"})
    void roomTemperatureLimitsAreExplicit(String celsius) {
        house().whenCommand(new SetRoomTemperature(HEAT, new BigDecimal(celsius)))
                .expectExceptionalResult(HomeRuleViolation.class).expectNoEvents();
    }
}
