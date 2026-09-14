package io.fluxzero.home;

import io.fluxzero.home.command.AddDevice;
import io.fluxzero.home.command.AddSpace;
import io.fluxzero.home.command.CreateHome;
import io.fluxzero.home.command.DefineAutomation;
import io.fluxzero.home.command.DefineScene;
import io.fluxzero.home.command.RemoveDevice;
import io.fluxzero.home.model.AutomationDetails;
import io.fluxzero.home.model.AutomationTrigger;
import io.fluxzero.home.model.Capability;
import io.fluxzero.home.model.DeviceDetails;
import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.DimLights;
import io.fluxzero.home.model.HomeBecomes;
import io.fluxzero.home.model.HomeDetails;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.home.model.HomeMode;
import io.fluxzero.home.model.LightLevel;
import io.fluxzero.home.model.Measurement;
import io.fluxzero.home.model.MeasurementCrosses;
import io.fluxzero.home.model.OneDevice;
import io.fluxzero.home.model.SceneDetails;
import io.fluxzero.home.model.SceneId;
import io.fluxzero.home.model.SpaceDetails;
import io.fluxzero.home.model.SpaceId;
import io.fluxzero.home.model.SpaceKind;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.test.TestFixture;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static io.fluxzero.home.HouseExample.*;
import static io.fluxzero.home.model.MeasurementCrosses.Direction.RISES_ABOVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AutomationDefinitionTest {
    private static final HomeId OTHER_HOME = new HomeId("other-home");
    private static final SpaceId OTHER_ROOM = new SpaceId("other-room");
    private static final DeviceId OTHER_DEVICE = new DeviceId("other-device");
    private static final SceneId OTHER_SCENE = new SceneId("other-scene");

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void sensorMustExist(boolean async) {
        (async ? asyncHouse() : house()).givenCommands(evening())
                .whenCommand(define(crossing(new DeviceId("missing"), Measurement.TEMPERATURE)))
                .expectExceptionalResult(IllegalCommandException.class).expectNoEvents()
                .expectThat(f -> assertNull(Fluxzero.loadModel(REACTION).get()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void sensorMustBelongToTheAutomationsHome(boolean async) {
        twoHomes(async).whenCommand(define(crossing(OTHER_DEVICE, Measurement.TEMPERATURE)))
                .expectExceptionalResult(IllegalCommandException.class).expectNoEvents()
                .expectThat(f -> assertNull(Fluxzero.loadModel(REACTION).get()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void sensorMustSupplyTheChosenMeasurement(boolean async) {
        (async ? asyncHouse() : house()).givenCommands(evening())
                .whenCommand(define(crossing(SENSOR, Measurement.HUMIDITY)))
                .expectExceptionalResult(IllegalCommandException.class).expectNoEvents()
                .expectThat(f -> assertNull(Fluxzero.loadModel(REACTION).get()));
    }

    @Test
    void sceneMustBelongToTheAutomationsHome() {
        twoHomes(false).whenCommand(new DefineAutomation(REACTION, HOME, new AutomationDetails("Leaving"),
                        OTHER_SCENE, new HomeBecomes(HomeMode.AWAY), Duration.ZERO))
                .expectExceptionalResult(IllegalCommandException.class).expectNoEvents()
                .expectThat(f -> assertNull(Fluxzero.loadModel(REACTION).get()));
    }

    @Test
    void redefiningAnAutomationCannotMoveItToAnotherHome() {
        twoHomes(false).givenCommands(define(new HomeBecomes(HomeMode.AWAY)))
                .whenCommand(new DefineAutomation(REACTION, OTHER_HOME, new AutomationDetails("Leaving"),
                        OTHER_SCENE, new HomeBecomes(HomeMode.AWAY), Duration.ZERO))
                .expectExceptionalResult(IllegalCommandException.class).expectNoEvents()
                .expectThat(f -> assertEquals(HOME, Fluxzero.loadModel(REACTION).get().homeId()));
    }

    @Test
    void referencedSensorCannotBeRemoved() {
        house().givenCommands(evening(), define(crossing(SENSOR, Measurement.TEMPERATURE)))
                .whenCommand(new RemoveDevice(SENSOR))
                .expectExceptionalResult(IllegalCommandException.class).expectNoEvents()
                .expectThat(f -> assertNotNull(Fluxzero.loadModel(SENSOR).get()));
    }

    private static DefineAutomation define(AutomationTrigger trigger) {
        return new DefineAutomation(REACTION, HOME, new AutomationDetails("Reaction"), EVENING, trigger, Duration.ZERO);
    }

    private static MeasurementCrosses crossing(DeviceId deviceId, Measurement measurement) {
        return new MeasurementCrosses(deviceId, measurement, RISES_ABOVE, new BigDecimal("24"));
    }

    private static TestFixture twoHomes(boolean async) {
        return (async ? asyncHouse() : house()).givenCommands(evening(),
                new CreateHome(OTHER_HOME, new HomeDetails("Other home"), AMSTERDAM),
                new AddSpace(OTHER_ROOM, OTHER_HOME, new SpaceDetails("Other room", SpaceKind.ROOM)),
                new AddDevice(OTHER_DEVICE, OTHER_ROOM, new DeviceDetails("Other device"), null,
                        Set.of(Capability.LIGHT_LEVEL), Set.of(Measurement.TEMPERATURE)),
                new DefineScene(OTHER_SCENE, OTHER_HOME, new SceneDetails("Other evening"),
                        List.of(new DimLights(new OneDevice(OTHER_DEVICE), new LightLevel(30)))));
    }
}
