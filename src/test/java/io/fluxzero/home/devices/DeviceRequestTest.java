package io.fluxzero.home.devices;

import io.fluxzero.home.devices.api.DimLight;
import io.fluxzero.home.devices.api.ReportDeviceStatus;
import io.fluxzero.home.devices.api.SetRoomTemperature;
import io.fluxzero.home.devices.api.TurnOn;
import io.fluxzero.home.devices.api.model.Availability;
import io.fluxzero.home.devices.api.model.Capability;
import io.fluxzero.home.devices.api.model.DeviceSettings;
import io.fluxzero.home.devices.api.model.DeviceStatus;
import io.fluxzero.home.devices.api.model.LightLevel;
import io.fluxzero.home.devices.api.model.Power;
import io.fluxzero.home.devices.api.model.RoomTemperature;
import io.fluxzero.sdk.Fluxzero;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.fluxzero.home.HouseExample.*;
import static org.junit.jupiter.api.Assertions.*;

class DeviceRequestTest {
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void confirmedRequestsDoNotOutlivePhysicalControl(boolean async) {
        var confirmed = DeviceSettings.empty().with(new Power(true)).with(new LightLevel(40));
        var switchedOff = DeviceSettings.empty().with(new Power(false)).with(new LightLevel(0));
        (async ? asyncHouse() : house())
                .givenCommands(new TurnOn(LIGHT), new DimLight(LIGHT, new LightLevel(40)))
                .atFixedTime(NOW.plusSeconds(1))
                .givenCommands(new ReportDeviceStatus(LIGHT, NOW.plusSeconds(1), Availability.ONLINE, confirmed, Map.of()))
                .atFixedTime(NOW.plusSeconds(2))
                .whenCommand(new ReportDeviceStatus(LIGHT, NOW.plusSeconds(2), Availability.ONLINE, switchedOff, Map.of()))
                .expectNoErrors().expectThat(f -> {
                    f.cache().clear();
                    assertTrue(Fluxzero.loadModel(LIGHT).get().pendingSettings().isEmpty());
                    assertEquals(switchedOff, Fluxzero.loadModel(LIGHT, DeviceStatus.class).get().reportedSettings());
                }).andThen().whenCommand(new TurnOn(LIGHT))
                .expectOnlyEvents(new TurnOn(LIGHT)).expectThat(f ->
                        assertEquals(DeviceSettings.empty().with(new Power(true)), Fluxzero.loadModel(LIGHT).get().pendingSettings()));
    }

    @Test
    void aReportSampledBeforeTheRequestCannotConfirmIt() {
        house().atFixedTime(NOW.plusSeconds(2)).givenCommands(new TurnOn(LIGHT))
                .whenCommand(new ReportDeviceStatus(LIGHT, NOW.plusSeconds(1), Availability.ONLINE,
                        DeviceSettings.empty().with(new Power(true)), Map.of()))
                .expectNoErrors().expectThat(f -> assertEquals(new Power(true),
                        Fluxzero.loadModel(LIGHT).get().pendingSettings().get(Capability.POWER)));
    }

    @Test
    void aMatchingReportOlderThanTheCurrentObservationCannotFinishTheRequest() {
        house().givenCommands(new DimLight(LIGHT, new LightLevel(40))).atFixedTime(NOW.plusSeconds(3))
                .givenCommands(new ReportDeviceStatus(LIGHT, NOW.plusSeconds(2), Availability.ONLINE,
                        DeviceSettings.empty().with(new LightLevel(20)), Map.of()))
                .whenCommand(new ReportDeviceStatus(LIGHT, NOW.plusSeconds(1), Availability.ONLINE,
                        DeviceSettings.empty().with(new LightLevel(40)), Map.of()))
                .expectNoEvents().expectThat(f -> assertEquals(new LightLevel(40),
                        Fluxzero.loadModel(LIGHT).get().pendingSettings().get(Capability.LIGHT_LEVEL)));
    }

    @ParameterizedTest @ValueSource(strings = {"OFFLINE", "UNKNOWN"})
    void unavailableReportsCannotConfirmARequest(String availability) {
        house().givenCommands(new TurnOn(LIGHT)).atFixedTime(NOW.plusSeconds(1))
                .whenCommand(new ReportDeviceStatus(LIGHT, NOW.plusSeconds(1), Availability.valueOf(availability),
                        DeviceSettings.empty().with(new Power(true)), Map.of()))
                .expectNoErrors().expectThat(f -> assertEquals(new Power(true),
                        Fluxzero.loadModel(LIGHT).get().pendingSettings().get(Capability.POWER)));
    }

    @Test
    void eachConfirmedControlLeavesUnfinishedControlsPending() {
        house().givenCommands(new TurnOn(LIGHT), new DimLight(LIGHT, new LightLevel(40)))
                .atFixedTime(NOW.plusSeconds(1))
                .whenCommand(new ReportDeviceStatus(LIGHT, NOW.plusSeconds(1), Availability.ONLINE,
                        DeviceSettings.empty().with(new Power(true)).with(new LightLevel(20)), Map.of()))
                .expectNoErrors().expectThat(f -> assertEquals(DeviceSettings.empty().with(new LightLevel(40)),
                        Fluxzero.loadModel(LIGHT).get().pendingSettings()));
    }

    @Test
    void temperatureConfirmationComparesValuesRatherThanDecimalScale() {
        house().givenCommands(new SetRoomTemperature(HEAT, new RoomTemperature(new BigDecimal("21"))))
                .atFixedTime(NOW.plusSeconds(1))
                .whenCommand(new ReportDeviceStatus(HEAT, NOW.plusSeconds(1), Availability.ONLINE,
                        DeviceSettings.empty().with(new RoomTemperature(new BigDecimal("21.0"))), Map.of()))
                .expectNoErrors().expectThat(f -> assertTrue(Fluxzero.loadModel(HEAT).get().pendingSettings().isEmpty()));
    }
}
