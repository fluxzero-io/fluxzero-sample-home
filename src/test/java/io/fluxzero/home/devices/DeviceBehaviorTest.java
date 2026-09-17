package io.fluxzero.home.devices;

import io.fluxzero.home.devices.api.AddDevice;
import io.fluxzero.home.devices.api.DeviceCommand;
import io.fluxzero.home.devices.api.DimLight;
import io.fluxzero.home.devices.api.EnableCharging;
import io.fluxzero.home.devices.api.GetDeviceStatus;
import io.fluxzero.home.devices.api.LockDoor;
import io.fluxzero.home.devices.api.PauseCharging;
import io.fluxzero.home.devices.api.PlayMedia;
import io.fluxzero.home.devices.api.RemoveDevice;
import io.fluxzero.home.devices.api.ReportDeviceStatus;
import io.fluxzero.home.devices.api.SetFanSpeed;
import io.fluxzero.home.devices.api.SetLightColor;
import io.fluxzero.home.devices.api.SetOpening;
import io.fluxzero.home.devices.api.SetRoomTemperature;
import io.fluxzero.home.devices.api.SetVolume;
import io.fluxzero.home.devices.api.StartWatering;
import io.fluxzero.home.devices.api.StopMedia;
import io.fluxzero.home.devices.api.StopWatering;
import io.fluxzero.home.devices.api.TurnOff;
import io.fluxzero.home.devices.api.TurnOn;
import io.fluxzero.home.devices.api.UnlockDoor;
import io.fluxzero.home.devices.api.model.Availability;
import io.fluxzero.home.devices.api.model.Capability;
import io.fluxzero.home.devices.api.model.DeviceDetails;
import io.fluxzero.home.devices.api.model.DeviceSetting;
import io.fluxzero.home.devices.api.model.DeviceSettings;
import io.fluxzero.home.devices.api.model.DeviceStatus;
import io.fluxzero.home.devices.api.model.LightLevel;
import io.fluxzero.home.devices.api.model.Measurement;
import io.fluxzero.home.devices.api.model.Power;
import io.fluxzero.home.devices.api.model.RoomTemperature;
import io.fluxzero.home.household.api.AddSpace;
import io.fluxzero.home.household.api.CreateHome;
import io.fluxzero.home.household.api.model.HomeDetails;
import io.fluxzero.home.household.api.model.SpaceDetails;
import io.fluxzero.home.household.api.model.SpaceKind;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.common.Message;
import io.fluxzero.sdk.modeling.*;
import io.fluxzero.sdk.test.*;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;
import io.fluxzero.sdk.tracking.handling.validation.ValidationException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import static io.fluxzero.home.HouseExample.*;
import static org.junit.jupiter.api.Assertions.*;

class DeviceBehaviorTest {
    static Stream<DeviceCommand> intentions() {
        return Stream.of(new TurnOn(LIGHT), new TurnOff(LIGHT), new DimLight(LIGHT, new LightLevel(42)), new SetLightColor(LIGHT, 120, 70),
                new SetRoomTemperature(LIGHT, new RoomTemperature(new BigDecimal("20.5"))), new SetOpening(LIGHT, 30), new LockDoor(LIGHT),
                new UnlockDoor(LIGHT), new PlayMedia(LIGHT, "Evening jazz"), new StopMedia(LIGHT), new SetVolume(LIGHT, 30),
                new SetFanSpeed(LIGHT, 60), new StartWatering(LIGHT), new StopWatering(LIGHT), new EnableCharging(LIGHT), new PauseCharging(LIGHT));
    }
    @ParameterizedTest @MethodSource("intentions")
    void everydayCommandsKeepIntentSeparateFromObservation(DeviceCommand command) {
        TestFixture.create().givenCommands(new CreateHome(HOME, new HomeDetails("Home"), AMSTERDAM),
                new AddSpace(LIVING, HOME, new SpaceDetails("Living", SpaceKind.ROOM)),
                new AddDevice(LIGHT, LIVING, new DeviceDetails("Multifunction device"), null, EnumSet.allOf(Capability.class), Set.of()))
                .whenCommand(command).expectOnlyEvents(command).expectThat(f -> {
                    assertEquals(command.setting(), Fluxzero.loadModel(LIGHT).get().pendingSettings().get(command.setting().capability()));
                    assertNull(Fluxzero.loadModel(LIGHT, DeviceStatus.class).get());
                });
    }
    @ParameterizedTest @ValueSource(ints = {-1, 101})
    void invalidBrightnessLeavesDeviceUntouched(int percent) {
        house().whenCommand(new DimLight(LIGHT, new LightLevel(percent))).expectExceptionalResult(ValidationException.class)
                .expectNoEvents().expectThat(f -> assertTrue(Fluxzero.loadModel(LIGHT).get().pendingSettings().isEmpty()));
    }
    @Test void aLightCannotSetRoomTemperature() {
        house().whenCommand(new SetRoomTemperature(LIGHT, new RoomTemperature(new BigDecimal("21"))))
                .expectExceptionalResult(IllegalCommandException.class).expectNoEvents();
    }
    @Test void aNewerObservationWinsAndPendingRequestsRemainIndependent() {
        house().givenCommands(temperature(NOW.minusSeconds(10), "19"))
                .whenCommand(temperature(NOW, "20")).expectNoErrors()
                .andThen().whenCommand(temperature(NOW.minusSeconds(5), "18"))
                .expectNoEvents().expectThat(f -> {
                    assertEquals(new BigDecimal("20"), Fluxzero.loadModel(SENSOR, DeviceStatus.class).get().readings().get(Measurement.TEMPERATURE));
                    assertTrue(Fluxzero.loadModel(SENSOR).get().pendingSettings().isEmpty());
                });
    }
    @Test void aDuplicateObservationIsSuppressed() {
        house().givenCommands(temperature(NOW, "20")).whenCommand(temperature(NOW, "19"))
                .expectNoEvents().expectThat(f -> assertEquals(new BigDecimal("20"),
                        Fluxzero.loadModel(SENSOR, DeviceStatus.class).get().readings().get(Measurement.TEMPERATURE)));
    }
    @Test void futureObservationsAreRejected() {
        house().whenCommand(temperature(NOW.plusSeconds(1), "20")).expectExceptionalResult(IllegalCommandException.class).expectNoEvents();
    }
    @Test void observationTimeIsComparedWithPublicationTime() {
        house().whenCommand(new Message(temperature(NOW, "20")).withTimestamp(NOW.minusSeconds(1)))
                .expectExceptionalResult(IllegalCommandException.class).expectNoEvents();
    }
    @Test void zeroAndOneAreValidMotionReadings() {
        house().givenCommands(new ReportDeviceStatus(SENSOR, NOW.minusSeconds(1),
                        Availability.ONLINE, DeviceSettings.empty(), Map.of(Measurement.MOTION, BigDecimal.ZERO)))
                .whenCommand(new ReportDeviceStatus(SENSOR, NOW,
                        Availability.ONLINE, DeviceSettings.empty(), Map.of(Measurement.MOTION, BigDecimal.ONE)))
                .expectNoErrors();
    }
    @Test void unsupportedMeasurementsAreRejected() {
        house().whenCommand(new ReportDeviceStatus(SENSOR, NOW, Availability.ONLINE,
                DeviceSettings.empty(), Map.of(Measurement.HUMIDITY, new BigDecimal("50"))))
                .expectExceptionalResult(IllegalCommandException.class).expectNoEvents();
    }

    @Test void unchangedBrightnessDoesNotRepublishAfterAnotherSettingChanges() {
        house().givenCommands(new DimLight(LIGHT, new LightLevel(42)), new TurnOn(LIGHT))
                .whenCommand(new DimLight(LIGHT, new LightLevel(42))).expectNoEvents();
    }

    @Test void colorValuesAreValidatedBeforeCheckingDeviceSupport() {
        house().whenCommand(new SetLightColor(LIGHT, 360, 50))
                .expectExceptionalResult(ValidationException.class).expectNoEvents();
    }

    static Stream<DeviceSettings> invalidReports() {
        return Stream.of(new DeviceSettings(List.of(new LightLevel(101))),
                new DeviceSettings(List.of(new LightLevel(10), new LightLevel(20))),
                new DeviceSettings(Arrays.asList((DeviceSetting) null)), new DeviceSettings(null));
    }

    @ParameterizedTest @MethodSource("invalidReports")
    void invalidReportedSettingsCreateNoObservation(DeviceSettings settings) {
        house().whenCommand(new ReportDeviceStatus(LIGHT, NOW,
                        Availability.ONLINE, settings, Map.of()))
                .expectExceptionalResult(ValidationException.class).expectNoEvents();
    }

    @Test void aDeviceCannotReportAnUnsupportedSetting() {
        house().whenCommand(new ReportDeviceStatus(LIGHT, NOW,
                        Availability.ONLINE, new DeviceSettings(List.of(new RoomTemperature(new BigDecimal("21")))), Map.of()))
                .expectExceptionalResult(IllegalCommandException.class).expectNoEvents();
    }

    @Test void reportedSettingsSurviveReloadAndStaySeparateFromIntent() {
        var report = new DeviceSettings(List.of(new LightLevel(20), new Power(true)));
        house().givenCommands(new DimLight(LIGHT, new LightLevel(42)),
                        new ReportDeviceStatus(LIGHT, NOW,
                                Availability.ONLINE, report, Map.of()))
                .whenExecuting(f -> f.cache().clear()).expectNoErrors().expectThat(f -> {
                    assertEquals(report, Fluxzero.loadModel(LIGHT, DeviceStatus.class).get().reportedSettings());
                    assertEquals(new LightLevel(42), Fluxzero.loadModel(LIGHT).get().pendingSettings().get(Capability.LIGHT_LEVEL));
                });
    }

    @ParameterizedTest @ValueSource(strings = {"4.9", "35.1"})
    void roomTemperatureLimitsAreExplicit(String celsius) {
        house().whenCommand(new SetRoomTemperature(HEAT, new RoomTemperature(new BigDecimal(celsius))))
                .expectExceptionalResult(ValidationException.class).expectNoEvents();
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void observationUsesItsDeviceIdentityAndFollowsDeviceRemoval(boolean async) {
        (async ? asyncHouse() : house()).givenCommands(temperature(NOW, "20"))
                .whenQuery(new GetDeviceStatus(SENSOR))
                .expectResult((DeviceStatus status) -> status.deviceId().equals(SENSOR)
                        && status.readings().get(Measurement.TEMPERATURE).equals(new BigDecimal("20")))
                .andThen().whenCommand(new RemoveDevice(SENSOR)).expectNoErrors()
                .andThen().whenExecuting(f -> f.cache().clear()).expectNoErrors()
                .andThen().whenQuery(new GetDeviceStatus(SENSOR)).expectResult((DeviceStatus) null)
                .expectThat(f -> {
                    assertNull(Fluxzero.loadModel(SENSOR).get());
                    assertNotNull(Fluxzero.loadModel(LIGHT).get());
                });
    }

}
