package io.fluxzero.home;

import io.fluxzero.home.automation.HomeReactions;
import io.fluxzero.home.command.AddDevice;
import io.fluxzero.home.command.AddSpace;
import io.fluxzero.home.command.ChangeHomeMode;
import io.fluxzero.home.command.CreateHome;
import io.fluxzero.home.command.DefineAutomation;
import io.fluxzero.home.command.DefineScene;
import io.fluxzero.home.command.MoveDevice;
import io.fluxzero.home.command.MoveSpace;
import io.fluxzero.home.command.RemoveDevice;
import io.fluxzero.home.model.Automation;
import io.fluxzero.home.model.AutomationId;
import io.fluxzero.home.model.AutomationTrigger;
import io.fluxzero.home.model.Capability;
import io.fluxzero.home.model.Device;
import io.fluxzero.home.model.DeviceId;
import io.fluxzero.home.model.DeviceSetting;
import io.fluxzero.home.model.Home;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.home.model.HomeMode;
import io.fluxzero.home.model.SceneAction;
import io.fluxzero.home.model.SceneId;
import io.fluxzero.home.model.SceneTarget;
import io.fluxzero.home.model.SpaceId;
import io.fluxzero.home.model.SpaceKind;
import io.fluxzero.home.query.FindDevices;
import io.fluxzero.sdk.Fluxzero;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.fluxzero.home.HouseExample.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeQueryTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void homeScopedSearchFiltersComponentsWithoutPublicModelCollections(boolean asynchronous) {
        var otherHome = new HomeId("other-home");
        var otherRoom = new SpaceId("other-room");
        var otherLight = new DeviceId("other-light");
        (asynchronous ? asyncHouse() : house()).givenCommands(
                new CreateHome(otherHome, "Other home", AMSTERDAM),
                new AddSpace(otherRoom, otherHome, null, "Other room", SpaceKind.ROOM),
                new AddDevice(otherLight, otherRoom, "Other light", null,
                              Set.of(Capability.POWER, Capability.LIGHT_LEVEL), Set.of()))
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
        var trigger = new AutomationTrigger.HomeBecomes(HomeMode.AWAY);
        (asynchronous ? asyncHouse(new HomeReactions()) : house(new HomeReactions())).givenCommands(
                evening(), new DefineAutomation(REACTION, HOME, "Leaving home", EVENING, trigger, Duration.ZERO),
                new CreateHome(otherHome, "Other home", AMSTERDAM),
                new AddSpace(otherRoom, otherHome, null, "Other room", SpaceKind.ROOM),
                new AddDevice(otherLight, otherRoom, "Other light", null, Set.of(Capability.LIGHT_LEVEL), Set.of()),
                new DefineScene(otherScene, otherHome, "Other evening", List.of(
                        new SceneAction(new SceneTarget.OneDevice(otherLight), new DeviceSetting.LightLevel(10)))),
                new DefineAutomation(otherAutomation, otherHome, "Leaving other home", otherScene, trigger, Duration.ZERO))
                .whenCommand(new ChangeHomeMode(HOME, HomeMode.AWAY)).expectNoErrors()
                .expectThat(f -> {
                    assertTrue(Fluxzero.search(Automation.class).fetchAll().isEmpty());
                    assertEvening();
                    assertEquals(1, Fluxzero.loadModel(REACTION).get().executionCount());
                    assertEquals(0, Fluxzero.loadModel(otherAutomation).get().executionCount());
                    assertTrue(Fluxzero.loadModel(otherLight).get().desiredSettings().isEmpty());
                })
                .andThen().whenCommand(new ChangeHomeMode(otherHome, HomeMode.AWAY)).expectNoErrors()
                .expectThat(f -> {
                    assertEquals(1, Fluxzero.loadModel(REACTION).get().executionCount());
                    assertEquals(1, Fluxzero.loadModel(otherAutomation).get().executionCount());
                    assertEquals(new DeviceSetting.LightLevel(10),
                                 Fluxzero.loadModel(otherLight).get().desiredSettings().get(Capability.LIGHT_LEVEL));
                });
    }

    private static Set<DeviceId> ids(List<Device> devices) {
        return devices.stream().map(Device::deviceId).collect(Collectors.toSet());
    }
}
