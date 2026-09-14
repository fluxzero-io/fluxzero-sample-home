package io.fluxzero.home;

import io.fluxzero.home.command.*;
import io.fluxzero.home.model.*;
import io.fluxzero.sdk.test.TestFixture;
import io.fluxzero.sdk.tracking.handling.validation.ValidationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static io.fluxzero.home.HouseExample.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class InputValidationTest {
    static Stream<Arguments> invalidInputs() {
        var device = new DeviceDetails("Lamp");
        var zone = new ZoneId("downstairs");
        var trigger = new HomeBecomes(HomeMode.AWAY);
        return Stream.of(
                arguments("device must do or measure something", "capabilitiesOrMeasurements",
                        new AddDevice(LIGHT, LIVING, device, null, Set.of(), Set.of())),
                arguments("optional label cannot be blank", "validLabel",
                        new AddDevice(LIGHT, LIVING, device, " ", Set.of(Capability.POWER), Set.of())),
                arguments("capabilities must be present", "capabilities",
                        new AddDevice(LIGHT, LIVING, device, null, null, Set.of())),
                arguments("measurements must be present", "measurements",
                        new AddDevice(LIGHT, LIVING, device, null, Set.of(Capability.POWER), null)),
                arguments("capabilities cannot contain null", "capabilities",
                        new AddDevice(LIGHT, LIVING, device, null, Collections.singleton(null), Set.of())),
                arguments("measurements cannot contain null", "measurements",
                        new AddDevice(LIGHT, LIVING, device, null, Set.of(), Collections.singleton(null))),
                arguments("resident needs a role", "role",
                        new AddResident(new ResidentId("alex"), HOME, new ResidentDetails("Alex"), null)),
                arguments("home needs an identity", "homeId", new CreateHome(null, new HomeDetails("Home"), AMSTERDAM)),
                arguments("home needs a time zone", "timeZone", new CreateHome(HOME, new HomeDetails("Home"), null)),
                arguments("home mode is required", "mode", new ChangeHomeMode(HOME, null)),
                arguments("home name is required", "name", new RenameHome(HOME, null)),
                arguments("home name cannot be blank", "name", new RenameHome(HOME, " ")),
                arguments("home name has a length limit", "name", new RenameHome(HOME, "x".repeat(121))),
                arguments("space name is required", "name", new RenameSpace(LIVING, null)),
                arguments("space name cannot be blank", "name", new RenameSpace(LIVING, " ")),
                arguments("space name has a length limit", "name", new RenameSpace(LIVING, "x".repeat(121))),
                arguments("zone needs spaces", "spaces", new DefineZone(zone, HOME, new ZoneDetails("Downstairs"), Set.of())),
                arguments("zone spaces must be present", "spaces", new DefineZone(zone, HOME, new ZoneDetails("Downstairs"), null)),
                arguments("zone cannot contain a null space", "spaces",
                        new DefineZone(zone, HOME, new ZoneDetails("Downstairs"), Collections.singleton(null))),
                arguments("space cannot be its own parent", "differentParent", new MoveSpace(LIVING, LIVING)),
                arguments("space requires an identity", "spaceId", new AddSpace(null, HOME, new SpaceDetails("Room", SpaceKind.ROOM))),
                arguments("space requires a parent", "parentId", new AddSpace(LIVING, null, new SpaceDetails("Room", SpaceKind.ROOM))),
                arguments("moving a space requires a parent", "parentId", new MoveSpace(LIVING, null)),
                arguments("device destination is required", "destinationId", new MoveDevice(LIGHT, null)),
                arguments("automation needs a trigger", "trigger", automation(null, Duration.ZERO)),
                arguments("automation needs a cooldown", "cooldown", automation(trigger, null)),
                arguments("cooldown cannot be negative", "nonNegativeCooldown", automation(trigger, Duration.ofSeconds(-1))),
                arguments("home trigger needs a mode", "trigger.mode", automation(new HomeBecomes(null), Duration.ZERO)),
                arguments("sensor trigger needs a device", "trigger.deviceId",
                        automation(new MeasurementCrosses(null, Measurement.TEMPERATURE,
                                MeasurementCrosses.Direction.RISES_ABOVE, BigDecimal.TEN), Duration.ZERO)),
                arguments("sensor trigger needs a measurement", "trigger.measurement",
                        automation(new MeasurementCrosses(SENSOR, null,
                                MeasurementCrosses.Direction.RISES_ABOVE, BigDecimal.TEN), Duration.ZERO)),
                arguments("sensor trigger needs a direction", "trigger.direction",
                        automation(new MeasurementCrosses(SENSOR, Measurement.TEMPERATURE, null, BigDecimal.TEN), Duration.ZERO)),
                arguments("sensor trigger needs a threshold", "trigger.threshold",
                        automation(new MeasurementCrosses(SENSOR, Measurement.TEMPERATURE,
                                MeasurementCrosses.Direction.RISES_ABOVE, null), Duration.ZERO)),
                arguments("threshold must fit its measurement", "trigger.validThreshold",
                        automation(new MeasurementCrosses(SENSOR, Measurement.HUMIDITY,
                                MeasurementCrosses.Direction.RISES_ABOVE, new BigDecimal("101")), Duration.ZERO)),
                arguments("routine needs timing", "timing", routine(null)),
                arguments("once needs a moment", "timing.at", routine(new Once(null))),
                arguments("weekly needs days", "timing.days", routine(new Weekly(Set.of(), LocalTime.NOON))),
                arguments("weekly days must be present", "timing.days", routine(new Weekly(null, LocalTime.NOON))),
                arguments("weekly cannot contain a null day", "timing.days",
                        routine(new Weekly(Collections.singleton(null), LocalTime.NOON))),
                arguments("weekly needs a local time", "timing.time", routine(new Weekly(Set.of(DayOfWeek.MONDAY), null))),
                arguments("status identity must match device", "matchingIdentity",
                        new ReportDeviceStatus(new DeviceStatusId("another"), SENSOR, NOW, Availability.ONLINE, DeviceSettings.empty(), Map.of())),
                arguments("status needs availability", "availability",
                        new ReportDeviceStatus(new DeviceStatusId(SENSOR.getFunctionalId()), SENSOR, NOW, null, DeviceSettings.empty(), Map.of())),
                arguments("status needs observation time", "observedAt",
                        new ReportDeviceStatus(new DeviceStatusId(SENSOR.getFunctionalId()), SENSOR, null, Availability.ONLINE, DeviceSettings.empty(), Map.of())),
                arguments("readings must be present", "readings", report(null)),
                arguments("reading kind must be present", "readings", report(Collections.singletonMap(null, BigDecimal.ONE))),
                arguments("reading value must be present", "readings", report(Collections.singletonMap(Measurement.TEMPERATURE, null))),
                arguments("percentage readings cannot exceed 100", "validReadings", report(Map.of(Measurement.HUMIDITY, new BigDecimal("101")))),
                arguments("boolean readings are zero or one", "validReadings", report(Map.of(Measurement.MOTION, new BigDecimal("0.5")))),
                arguments("illuminance cannot be negative", "validReadings", report(Map.of(Measurement.ILLUMINANCE, BigDecimal.ONE.negate())))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidInputs")
    void malformedInputIsReportedEvenWhenTheReferencedHomeDoesNotExist(String rule, String path, Object command) {
        TestFixture.create().atFixedTime(NOW).whenCommand(command)
                .expectExceptionalResult((ValidationException error) -> error.getViolationSummaries().stream()
                        .anyMatch(violation -> violation.path().startsWith(path)), "Validation error at " + path)
                .expectNoEvents().expectNoSchedules();
    }

    private static DefineAutomation automation(AutomationTrigger trigger, Duration cooldown) {
        return new DefineAutomation(REACTION, HOME, new AutomationDetails("Reaction"), EVENING, trigger, cooldown);
    }

    private static PlanRoutine routine(RoutineTiming timing) {
        return new PlanRoutine(BEDTIME, HOME, new RoutineDetails("Evening"), EVENING, timing);
    }

    private static ReportDeviceStatus report(Map<Measurement, BigDecimal> readings) {
        return new ReportDeviceStatus(new DeviceStatusId(SENSOR.getFunctionalId()), SENSOR, NOW,
                Availability.ONLINE, DeviceSettings.empty(), readings);
    }
}
