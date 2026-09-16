package io.fluxzero.home.household;

import io.fluxzero.home.automation.HomeReactions;
import io.fluxzero.home.automation.api.AutomationId;
import io.fluxzero.home.automation.api.DefineAutomation;
import io.fluxzero.home.automation.api.model.Automation;
import io.fluxzero.home.automation.api.model.AutomationDetails;
import io.fluxzero.home.automation.api.model.HomeBecomes;
import io.fluxzero.home.devices.api.AddDevice;
import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.devices.api.FindDevices;
import io.fluxzero.home.devices.api.MoveDevice;
import io.fluxzero.home.devices.api.RemoveDevice;
import io.fluxzero.home.devices.api.model.Capability;
import io.fluxzero.home.devices.api.model.Device;
import io.fluxzero.home.devices.api.model.DeviceDetails;
import io.fluxzero.home.devices.api.model.LightLevel;
import io.fluxzero.home.household.api.AddSpace;
import io.fluxzero.home.household.api.ChangeHomeMode;
import io.fluxzero.home.household.api.CreateHome;
import io.fluxzero.home.household.api.HomeId;
import io.fluxzero.home.household.api.MoveSpace;
import io.fluxzero.home.household.api.SpaceId;
import io.fluxzero.home.household.api.model.Home;
import io.fluxzero.home.household.api.model.HomeDetails;
import io.fluxzero.home.household.api.model.HomeMode;
import io.fluxzero.home.household.api.model.SpaceDetails;
import io.fluxzero.home.household.api.model.SpaceKind;
import io.fluxzero.home.scenes.api.DefineScene;
import io.fluxzero.home.scenes.api.SceneId;
import io.fluxzero.home.scenes.api.model.DimLights;
import io.fluxzero.home.scenes.api.model.OneDevice;
import io.fluxzero.home.scenes.api.model.SceneDetails;
import io.fluxzero.sdk.Fluxzero;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.fluxzero.home.HouseExample.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeQueryTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void homeScopedSearchFiltersComponentsWithoutPublicModelCollections(boolean asynchronous) {
        var otherHome = new HomeId("other-home");
        var otherRoom = new SpaceId("other-room");
        var otherLight = new DeviceId("other-light");
        (asynchronous ? asyncHouse() : house()).givenCommands(
                new CreateHome(otherHome, new HomeDetails("Other home"), AMSTERDAM),
                new AddSpace(otherRoom, otherHome, new SpaceDetails("Other room", SpaceKind.ROOM)),
                new AddDevice(otherLight, otherRoom, new DeviceDetails("Other light"), null, Set.of(Capability.POWER, Capability.LIGHT_LEVEL), Set.of()))
                .whenQuery(new FindDevices(HOME, Capability.LIGHT_LEVEL))
                .expectResult((List<Device> devices) -> ids(devices).equals(Set.of(LIGHT)))
                .expectThat(f -> {
                    assertTrue(Fluxzero.search(Home.class).fetchAll().isEmpty());
                    assertTrue(Fluxzero.search(Device.class).fetchAll().isEmpty());
                })
                .andThen().whenQuery(new FindDevices(HOME, null))
                .expectResult((List<Device> devices) -> ids(devices).equals(Set.of(LIGHT, HEAT, SENSOR)))
                .andThen().whenQuery(new FindDevices(otherHome, Capability.LIGHT_LEVEL))
                .expectResult((List<Device> devices) -> ids(devices).equals(Set.of(otherLight)))
                .andThen().whenQuery(new FindDevices(new HomeId("missing"), null))
                .expectResult(List.of());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void homeScopedSearchTracksMovesAndRemovalAfterCacheEviction(boolean asynchronous) {
        (asynchronous ? asyncHouse() : house())
                .givenCommands(new MoveSpace(LIVING, GARDEN), new MoveDevice(HEAT, FLOOR))
                .whenExecuting(f -> f.cache().clear()).expectNoErrors()
                .andThen().whenQuery(new FindDevices(HOME, null))
                .expectResult((List<Device> devices) -> devices.size() == 3
                        && ids(devices).equals(Set.of(LIGHT, HEAT, SENSOR)))
                .andThen().whenQuery(new FindDevices(HOME, Capability.TEMPERATURE))
                .expectResult((List<Device> devices) -> ids(devices).equals(Set.of(HEAT))
                        && devices.getFirst().spaceId().equals(FLOOR))
                .andThen().whenCommand(new RemoveDevice(LIGHT)).expectNoErrors()
                .andThen().whenExecuting(f -> f.cache().clear()).expectNoErrors()
                .andThen().whenQuery(new FindDevices(HOME, Capability.LIGHT_LEVEL))
                .expectResult(List.of())
                .andThen().whenQuery(new FindDevices(HOME, null))
                .expectResult((List<Device> devices) -> ids(devices).equals(Set.of(HEAT, SENSOR)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void modeChangesSelectOnlyTheirHomesAutomationWithoutAPublicCollection(boolean asynchronous) {
        var otherHome = new HomeId("other-home");
        var otherRoom = new SpaceId("other-room");
        var otherLight = new DeviceId("other-light");
        var otherScene = new SceneId("other-evening");
        var otherAutomation = new AutomationId("other-reaction");
        var trigger = new HomeBecomes(HomeMode.AWAY);
        (asynchronous ? asyncHouse(new HomeReactions()) : house(new HomeReactions())).givenCommands(
                evening(), new DefineAutomation(REACTION, HOME, new AutomationDetails("Leaving home"), EVENING, trigger, Duration.ZERO),
                new CreateHome(otherHome, new HomeDetails("Other home"), AMSTERDAM),
                new AddSpace(otherRoom, otherHome, new SpaceDetails("Other room", SpaceKind.ROOM)),
                new AddDevice(otherLight, otherRoom, new DeviceDetails("Other light"), null, Set.of(Capability.LIGHT_LEVEL), Set.of()),
                new DefineScene(otherScene, otherHome, new SceneDetails("Other evening"), List.of(
                        new DimLights(new OneDevice(otherLight), new LightLevel(10)))),
                new DefineAutomation(otherAutomation, otherHome, new AutomationDetails("Leaving other home"), otherScene, trigger, Duration.ZERO))
                .whenCommand(new ChangeHomeMode(HOME, HomeMode.AWAY)).expectNoErrors()
                .expectThat(f -> {
                    assertTrue(Fluxzero.search(Automation.class).fetchAll().isEmpty());
                    assertEvening();
                    assertNotNull(Fluxzero.loadModel(REACTION).get().cooldownEndsAt());
                    assertNull(Fluxzero.loadModel(otherAutomation).get().cooldownEndsAt());
                    assertTrue(Fluxzero.loadModel(otherLight).get().desiredSettings().isEmpty());
                })
                .andThen().whenCommand(new ChangeHomeMode(otherHome, HomeMode.AWAY)).expectNoErrors()
                .expectThat(f -> {
                    assertNotNull(Fluxzero.loadModel(REACTION).get().cooldownEndsAt());
                    assertNotNull(Fluxzero.loadModel(otherAutomation).get().cooldownEndsAt());
                    assertEquals(new LightLevel(10),
                                 Fluxzero.loadModel(otherLight).get().desiredSettings().get(Capability.LIGHT_LEVEL));
                });
    }

    private static Set<DeviceId> ids(List<Device> devices) {
        return devices.stream().map(Device::deviceId).collect(Collectors.toSet());
    }
}
