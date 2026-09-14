package io.fluxzero.home;

import io.fluxzero.home.automation.*;
import io.fluxzero.home.command.*;
import io.fluxzero.home.model.*;
import io.fluxzero.home.query.*;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.modeling.*;
import io.fluxzero.sdk.scheduling.Schedule;
import io.fluxzero.sdk.test.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.fluxzero.home.HouseExample.*;
import static org.junit.jupiter.api.Assertions.*;

/** A compact, ordinary house used throughout the executable examples. */
final class HouseExample {
    static final HomeId HOME = new HomeId("canal-house");
    static final SpaceId FLOOR = new SpaceId("ground-floor");
    static final SpaceId LIVING = new SpaceId("living-room");
    static final SpaceId GARDEN = new SpaceId("garden");
    static final DeviceId LIGHT = new DeviceId("reading-light");
    static final DeviceId HEAT = new DeviceId("heating");
    static final DeviceId SENSOR = new DeviceId("room-sensor");
    static final SceneId EVENING = new SceneId("evening");
    static final RoutineId BEDTIME = new RoutineId("bedtime");
    static final AutomationId REACTION = new AutomationId("reaction");
    static final Instant NOW = Instant.parse("2026-09-14T10:00:00Z");
    static final java.time.ZoneId AMSTERDAM = java.time.ZoneId.of("Europe/Amsterdam");
    static TestFixture house(Object... handlers) {
        return populate(TestFixture.create(handlers));
    }
    static TestFixture asyncHouse(Object... handlers) {
        return populate(TestFixture.createAsync(handlers));
    }
    static TestFixture populate(TestFixture fixture) {
        return fixture.atFixedTime(NOW).withProperty("fluxzero.defaults.version", "2026.09.10")
            .givenCommands(new CreateHome(HOME, new HomeDetails("Canal house"), AMSTERDAM),
                new AddSpace(FLOOR, HOME, null, new SpaceDetails("Ground floor", SpaceKind.FLOOR)),
                new AddSpace(LIVING, HOME, FLOOR, new SpaceDetails("Living room", SpaceKind.ROOM)),
                new AddSpace(GARDEN, HOME, null, new SpaceDetails("Garden", SpaceKind.OUTDOOR)),
                new AddDevice(LIGHT, LIVING, new DeviceDetails("Reading light"), "reading-light", Set.of(Capability.POWER, Capability.LIGHT_LEVEL), Set.of()),
                new AddDevice(HEAT, LIVING, new DeviceDetails("Heating"), null, Set.of(Capability.TEMPERATURE), Set.of()),
                new AddDevice(SENSOR, LIVING, new DeviceDetails("Room sensor"), null, Set.of(), Set.of(Measurement.TEMPERATURE, Measurement.MOTION)));
    }
    static DefineScene evening() {
        return new DefineScene(EVENING, HOME, new SceneDetails("A comfortable evening"), List.of(
            new DimLights(new InSpace(LIVING), new LightLevel(25)),
            new SetHeating(new InSpace(LIVING), new RoomTemperature(new BigDecimal("21")))));
    }
    static ReportDeviceStatus temperature(Instant at, String value) {
        return new ReportDeviceStatus(new DeviceStatusId(SENSOR.getFunctionalId()), SENSOR, at, Availability.ONLINE,
                DeviceSettings.empty(), Map.of(Measurement.TEMPERATURE, new BigDecimal(value)));
    }
    static PlanRoutine once(Instant due) {
        return new PlanRoutine(BEDTIME, HOME, new RoutineDetails("Evening comfort"), EVENING, new RoutineTiming.Once(due));
    }
    static Predicate<Schedule> scheduled(long generation, Instant due) {
        return s -> s.getScheduleId().equals(RoutineSchedules.scheduleId(BEDTIME).toString()) && s.getDeadline().equals(due)
                && new RunRoutine(BEDTIME, generation, due).equals(s.getPayload());
    }
    static void assertEvening() {
        assertEquals(new LightLevel(25), Fluxzero.loadModel(LIGHT).get().desiredSettings().get(Capability.LIGHT_LEVEL));
        assertEquals(new RoomTemperature(new BigDecimal("21")), Fluxzero.loadModel(HEAT).get().desiredSettings().get(Capability.TEMPERATURE));
    }
}
