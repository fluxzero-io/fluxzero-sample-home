package io.fluxzero.home;

import io.fluxzero.home.automation.RoutineSchedules;
import io.fluxzero.home.automation.api.AutomationId;
import io.fluxzero.home.automation.api.PlanRoutine;
import io.fluxzero.home.automation.api.RoutineId;
import io.fluxzero.home.automation.api.RunRoutine;
import io.fluxzero.home.automation.api.model.Once;
import io.fluxzero.home.automation.api.model.RoutineDetails;
import io.fluxzero.home.devices.api.AddDevice;
import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.devices.api.ReportDeviceStatus;
import io.fluxzero.home.devices.api.model.Availability;
import io.fluxzero.home.devices.api.model.Capability;
import io.fluxzero.home.devices.api.model.DeviceDetails;
import io.fluxzero.home.devices.api.model.DeviceSettings;
import io.fluxzero.home.devices.api.model.LightLevel;
import io.fluxzero.home.devices.api.model.Measurement;
import io.fluxzero.home.devices.api.model.RoomTemperature;
import io.fluxzero.home.household.api.AddSpace;
import io.fluxzero.home.household.api.CreateHome;
import io.fluxzero.home.household.api.HomeId;
import io.fluxzero.home.household.api.SpaceId;
import io.fluxzero.home.household.api.model.HomeDetails;
import io.fluxzero.home.household.api.model.SpaceDetails;
import io.fluxzero.home.household.api.model.SpaceKind;
import io.fluxzero.home.scenes.api.DefineScene;
import io.fluxzero.home.scenes.api.SceneId;
import io.fluxzero.home.scenes.api.model.DimLights;
import io.fluxzero.home.scenes.api.model.InSpace;
import io.fluxzero.home.scenes.api.model.SceneDetails;
import io.fluxzero.home.scenes.api.model.SetHeating;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.modeling.*;
import io.fluxzero.sdk.scheduling.Schedule;
import io.fluxzero.sdk.test.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Predicate;
import org.junit.jupiter.params.provider.*;

import static org.junit.jupiter.api.Assertions.*;

/** A compact, ordinary house used throughout the executable examples. */
public final class HouseExample {
    public static final HomeId HOME = new HomeId("canal-house");
    public static final SpaceId FLOOR = new SpaceId("ground-floor");
    public static final SpaceId LIVING = new SpaceId("living-room");
    public static final SpaceId GARDEN = new SpaceId("garden");
    public static final DeviceId LIGHT = new DeviceId("reading-light");
    public static final DeviceId HEAT = new DeviceId("heating");
    public static final DeviceId SENSOR = new DeviceId("room-sensor");
    public static final SceneId EVENING = new SceneId("evening");
    public static final RoutineId BEDTIME = new RoutineId("bedtime");
    public static final AutomationId REACTION = new AutomationId("reaction");
    public static final Instant NOW = Instant.parse("2026-09-14T10:00:00Z");
    public static final java.time.ZoneId AMSTERDAM = java.time.ZoneId.of("Europe/Amsterdam");
    public static TestFixture house(Object... handlers) {
        return populate(TestFixture.create(handlers));
    }
    public static TestFixture asyncHouse(Object... handlers) {
        return populate(TestFixture.createAsync(handlers));
    }
    public static TestFixture populate(TestFixture fixture) {
        return fixture.atFixedTime(NOW).withProperty("fluxzero.defaults.version", "2026.09.10")
            .givenCommands(new CreateHome(HOME, new HomeDetails("Canal house"), AMSTERDAM),
                new AddSpace(FLOOR, HOME, new SpaceDetails("Ground floor", SpaceKind.FLOOR)),
                new AddSpace(LIVING, FLOOR, new SpaceDetails("Living room", SpaceKind.ROOM)),
                new AddSpace(GARDEN, HOME, new SpaceDetails("Garden", SpaceKind.OUTDOOR)),
                new AddDevice(LIGHT, LIVING, new DeviceDetails("Reading light"), "reading-light", Set.of(Capability.POWER, Capability.LIGHT_LEVEL), Set.of()),
                new AddDevice(HEAT, LIVING, new DeviceDetails("Heating"), null, Set.of(Capability.TEMPERATURE), Set.of()),
                new AddDevice(SENSOR, LIVING, new DeviceDetails("Room sensor"), null, Set.of(), Set.of(Measurement.TEMPERATURE, Measurement.MOTION)));
    }
    public static DefineScene evening() {
        return new DefineScene(EVENING, HOME, new SceneDetails("A comfortable evening"), List.of(
            new DimLights(new InSpace(LIVING), new LightLevel(25)),
            new SetHeating(new InSpace(LIVING), new RoomTemperature(new BigDecimal("21")))));
    }
    public static ReportDeviceStatus temperature(Instant at, String value) {
        return new ReportDeviceStatus(SENSOR, at, Availability.ONLINE,
                DeviceSettings.empty(), Map.of(Measurement.TEMPERATURE, new BigDecimal(value)));
    }
    public static PlanRoutine once(Instant due) {
        return new PlanRoutine(BEDTIME, HOME, new RoutineDetails("Evening comfort"), EVENING, new Once(due));
    }
    public static Predicate<Schedule> scheduled(long generation, Instant due) {
        return s -> s.getScheduleId().equals(RoutineSchedules.scheduleId(BEDTIME).toString()) && s.getDeadline().equals(due)
                && new RunRoutine(BEDTIME, generation, due).equals(s.getPayload());
    }
    public static void assertEvening() {
        assertEquals(new LightLevel(25), Fluxzero.loadModel(LIGHT).get().pendingSettings().get(Capability.LIGHT_LEVEL));
        assertEquals(new RoomTemperature(new BigDecimal("21")), Fluxzero.loadModel(HEAT).get().pendingSettings().get(Capability.TEMPERATURE));
    }
}
