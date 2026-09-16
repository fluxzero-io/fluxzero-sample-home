package io.fluxzero.home.homeassistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fluxzero.home.model.*;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HomeAssistantTranslationTest {
    @ParameterizedTest
    @CsvSource({"temperature,°F,77,25", "temperature,°C,21.5,21.5", "power,kW,1.5,1500", "energy,Wh,1250,1.25",
            "humidity,%,45,45", "illuminance,lx,150,150", "battery,%,80,80", "carbon_dioxide,ppm,800,800"})
    void readingsUseTheHomeModelsCanonicalUnits(String kind, String unit, String raw, String expected) throws Exception {
        var state = sensor(kind, unit, raw);
        assertEquals(0, new BigDecimal(expected).compareTo(state.reading()));
    }

    @ParameterizedTest @CsvSource({"on,turn_on", "off,turn_off"})
    void switchesUseServiceActions(String power, String service) throws Exception {
        var state = new HomeAssistantState("switch.fountain", "on", new ObjectMapper().readTree("{}"));
        assertEquals(List.of(new HomeAssistantAction("switch", service, new HomeAssistantAction.SwitchEntity("switch.fountain"))),
                state.actions(DeviceSettings.empty().with(new Power(power.equals("on"))), "°C"));
    }

    @Test
    void anInvalidReadingIsUnknownAndNeverInventedAsZero() throws Exception {
        var state = sensor("temperature", "°C", "not-a-number");
        var device = new Device(new DeviceId("sensor"), new SpaceId("room"), new DeviceDetails("Sensor"), null,
                Set.of(), Set.of(Measurement.TEMPERATURE), DeviceSettings.empty());
        var report = new HomeAssistantSnapshot(List.of(state), "°C").observe(device, Set.of(state.entityId()), Instant.EPOCH);
        assertEquals(Availability.UNKNOWN, report.availability());
        assertTrue(report.readings().isEmpty());
    }

    @Test
    void unknownUnitsAreNotAdvertisedAsSupportedMeasurements() throws Exception {
        assertTrue(sensor("temperature", "unknown-unit", "20").describe().measurements().isEmpty());
    }

    @Test
    void twoSourcesForOneMeasurementAreAmbiguous() throws Exception {
        var first = sensor("temperature", "°C", "20");
        var second = new HomeAssistantState("sensor.other", "21", first.attributes());
        var device = new Device(new DeviceId("sensor"), new SpaceId("room"), new DeviceDetails("Sensor"), null,
                Set.of(), Set.of(Measurement.TEMPERATURE), DeviceSettings.empty());
        assertThrows(IllegalCommandException.class, () ->
                new HomeAssistantSnapshot(List.of(first, second), "°C").validateBinding(device, Set.of(first.entityId(), second.entityId())));
    }

    private HomeAssistantState sensor(String kind, String unit, String state) throws Exception {
        return new HomeAssistantState("sensor.example", state, new ObjectMapper().readTree(
                "{\"device_class\":\"%s\",\"unit_of_measurement\":\"%s\"}".formatted(kind, unit)));
    }

    @Test
    void onlyPositionableCoversAndSingleSetpointThermostatsAreAdvertised() throws Exception {
        assertTrue(entity("cover.door", "closed", "{\"supported_features\":3}").capabilities().isEmpty());
        assertTrue(entity("climate.range", "heat_cool", "{\"supported_features\":2}").capabilities().isEmpty());
        assertEquals(Set.of(Capability.OPENING), entity("cover.blinds", "open", "{\"supported_features\":15}").capabilities());
        assertEquals(Set.of(Capability.TEMPERATURE), entity("climate.heat", "heat", "{\"supported_features\":385}").capabilities());
    }

    @ParameterizedTest @ValueSource(strings = {"null", "-1", "101", "\"50\""})
    void unknownCoverPositionsAreNeverInvented(String position) throws Exception {
        var cover = entity("cover.blinds", "open", "{\"supported_features\":15,\"current_position\":" + position + "}");
        assertTrue(cover.settings("°C").isEmpty());
    }

    @Test
    void colorAndBrightnessShareOneActionButOffTakesPrecedence() throws Exception {
        var light = entity("light.color", "on", "{\"supported_color_modes\":[\"hs\"]}");
        var settings = DeviceSettings.empty().with(new LightColor(210, 70)).with(new LightLevel(40));
        assertEquals(List.of(new HomeAssistantAction("light", "turn_on",
                new HomeAssistantAction.ColorEntity("light.color", List.of(210, 70), 40))), light.actions(settings, null));
        assertEquals(List.of(new HomeAssistantAction("light", "turn_off",
                new HomeAssistantAction.SwitchEntity("light.color"))), light.actions(settings.with(new Power(false)), null));
    }

    @Test
    void whiteOnlyLightsDoNotPretendToSupportColor() throws Exception {
        var light = entity("light.white", "on", "{\"supported_color_modes\":[\"color_temp\"]}");
        assertEquals(Set.of(Capability.POWER, Capability.LIGHT_LEVEL), light.capabilities());
    }

    @Test
    void colorReadingsUseWholeDegreesAndNeverInventMissingColor() throws Exception {
        var attributes = "{\"supported_color_modes\":[\"hs\"],\"brightness\":128,\"hs_color\":[359.8,49.7]}";
        assertEquals(new LightColor(0, 50), entity("light.color", "on", attributes).settings(null).get(Capability.LIGHT_COLOR));
        assertNull(entity("light.color", "on", attributes.replace("[359.8,49.7]", "null"))
                .settings(null).get(Capability.LIGHT_COLOR));
    }

    @Test
    void thermostatLimitsAndRangeModeAreRespectedWithoutChangingTheMode() throws Exception {
        var heat = entity("climate.heat", "off", "{\"supported_features\":3,\"min_temp\":7,\"max_temp\":30,\"temperature\":20}");
        var desired = DeviceSettings.empty().with(new RoomTemperature(BigDecimal.valueOf(21)));
        assertEquals(List.of(new HomeAssistantAction("climate", "set_temperature",
                new HomeAssistantAction.SetTemperature("climate.heat", BigDecimal.valueOf(21)))), heat.actions(desired, "°C"));
        assertThrows(HomeAssistantUnavailable.class, () -> heat.actions(
                DeviceSettings.empty().with(new RoomTemperature(BigDecimal.valueOf(35))), "°C"));
        var rangeMode = new HomeAssistantState(heat.entityId(), "heat_cool", heat.attributes());
        assertThrows(HomeAssistantUnavailable.class, () -> rangeMode.actions(desired, "°C"));
        assertTrue(rangeMode.settings("°C").isEmpty());
    }

    private HomeAssistantState entity(String id, String state, String attributes) throws Exception {
        return new HomeAssistantState(id, state, new ObjectMapper().readTree(attributes));
    }
}
