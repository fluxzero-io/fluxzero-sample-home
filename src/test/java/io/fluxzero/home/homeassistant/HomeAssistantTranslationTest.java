package io.fluxzero.home.homeassistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fluxzero.home.model.*;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
                state.actions(DeviceSettings.empty().with(new Power(power.equals("on")))));
    }

    @Test
    void anInvalidReadingIsUnknownAndNeverInventedAsZero() throws Exception {
        var state = sensor("temperature", "°C", "not-a-number");
        var device = new Device(new DeviceId("sensor"), new SpaceId("room"), new DeviceDetails("Sensor"), null,
                Set.of(), Set.of(Measurement.TEMPERATURE), DeviceSettings.empty());
        var report = new HomeAssistantSnapshot(List.of(state)).observe(device, Set.of(state.entityId()), Instant.EPOCH);
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
                new HomeAssistantSnapshot(List.of(first, second)).validateBinding(device, Set.of(first.entityId(), second.entityId())));
    }

    private HomeAssistantState sensor(String kind, String unit, String state) throws Exception {
        return new HomeAssistantState("sensor.example", state, new ObjectMapper().readTree(
                "{\"device_class\":\"%s\",\"unit_of_measurement\":\"%s\"}".formatted(kind, unit)));
    }
}
