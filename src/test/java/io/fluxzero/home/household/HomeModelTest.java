package io.fluxzero.home.household;

import io.fluxzero.home.devices.api.AddDevice;
import io.fluxzero.home.devices.api.FindDevices;
import io.fluxzero.home.devices.api.MoveDevice;
import io.fluxzero.home.devices.api.RemoveDevice;
import io.fluxzero.home.devices.api.model.Capability;
import io.fluxzero.home.devices.api.model.Device;
import io.fluxzero.home.devices.api.model.DeviceDetails;
import io.fluxzero.home.household.api.AddResident;
import io.fluxzero.home.household.api.AddSpace;
import io.fluxzero.home.household.api.ArriveHome;
import io.fluxzero.home.household.api.ChoosePrimaryLight;
import io.fluxzero.home.household.api.CreateHome;
import io.fluxzero.home.household.api.DefineZone;
import io.fluxzero.home.household.api.GetHome;
import io.fluxzero.home.household.api.HomeId;
import io.fluxzero.home.household.api.LeaveHome;
import io.fluxzero.home.household.api.MoveSpace;
import io.fluxzero.home.household.api.RemoveHome;
import io.fluxzero.home.household.api.RemoveSpace;
import io.fluxzero.home.household.api.ResidentId;
import io.fluxzero.home.household.api.SpaceId;
import io.fluxzero.home.household.api.model.Home;
import io.fluxzero.home.household.api.model.HomeDetails;
import io.fluxzero.home.household.api.model.HouseholdRole;
import io.fluxzero.home.household.api.model.Place;
import io.fluxzero.home.household.api.model.Presence;
import io.fluxzero.home.household.api.model.ResidentDetails;
import io.fluxzero.home.household.api.model.Space;
import io.fluxzero.home.household.api.model.SpaceDetails;
import io.fluxzero.home.household.api.model.SpaceKind;
import io.fluxzero.home.household.api.model.ZoneDetails;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.modeling.*;
import io.fluxzero.sdk.test.*;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import static io.fluxzero.home.HouseExample.*;
import static org.junit.jupiter.api.Assertions.*;

class HomeModelTest {
    @Test void hierarchyAndIndependentQueries() {
        house().whenQuery(new GetHome(HOME)).expectResult((Graph<Home> graph) -> {
            assertEquals(2, graph.childModels(Space.class).size());
            assertEquals(3, graph.descendantModels(Space.class).size());
            assertEquals(3, graph.descendantModels(Device.class).size());
            assertEquals(HOME, graph.find(LIGHT, Device.class).orElseThrow().ancestor(Home.class).orElseThrow().get().id());
            return true;
        }).andThen().whenQuery(new FindDevices(HOME, Capability.LIGHT_LEVEL))
            .expectResult((List<Device> devices) -> devices.size() == 1 && devices.getFirst().deviceId().equals(LIGHT));
    }
    @Test void invalidNameCannotCreateHome() {
        TestFixture.create().whenCommand(new CreateHome(HOME, new HomeDetails(" "), AMSTERDAM))
                .expectExceptionalResult(io.fluxzero.sdk.tracking.handling.validation.ValidationException.class).expectNoEvents();
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void duplicateCreationCannotOverwriteHome(boolean async) {
        (async ? asyncHouse() : house()).whenCommand(new CreateHome(HOME, new HomeDetails("Another name"), AMSTERDAM))
                .expectExceptionalResult(Entity.ALREADY_EXISTS_EXCEPTION).expectNoEvents()
                .expectThat(f -> assertEquals("Canal house", Fluxzero.loadModel(HOME).get().details().name()));
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void spaceBelongsToItsChosenParent(boolean async) {
        var fixture = async ? asyncHouse() : house();
        fixture.whenCommand(new AddSpace(new SpaceId("attic"), HOME, new SpaceDetails("Attic", SpaceKind.ROOM)))
                .expectNoErrors().expectThat(f -> assertEquals(HOME, Fluxzero.loadModel(new SpaceId("attic")).get().parentId()))
                .andThen().whenCommand(new AddSpace(new SpaceId("closet"), LIVING, new SpaceDetails("Closet", SpaceKind.ROOM)))
                .expectNoErrors().expectThat(f -> {
                    f.cache().clear();
                    var closet = Fluxzero.loadGraph(new SpaceId("closet"));
                    assertEquals(LIVING, closet.get().parentId());
                    assertEquals(HOME, closet.ancestor(Home.class).orElseThrow().get().id());
                });
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void aNewSpaceNeedsAnExistingDestination(boolean async) {
        var closet = new SpaceId("closet");
        var details = new SpaceDetails("Closet", SpaceKind.ROOM);
        (async ? asyncHouse() : house())
                .whenCommand(new AddSpace(closet, new HomeId("missing"), details))
                .expectExceptionalResult(IllegalCommandException.class).expectNoEvents()
                .andThen().whenCommand(new AddSpace(closet, new SpaceId("missing"), details))
                .expectExceptionalResult(IllegalCommandException.class).expectNoEvents()
                .expectThat(f -> assertNull(Fluxzero.loadModel(closet).get()));
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void aSpaceCanMoveBackUnderItsHomeWithItsContents(boolean async) {
        (async ? asyncHouse() : house()).whenCommand(new MoveSpace(LIVING, HOME)).expectNoErrors()
                .expectThat(f -> {
                    f.cache().clear();
                    var home = Fluxzero.loadGraph(HOME);
                    assertEquals(3, home.childModels(Space.class).size());
                    assertTrue(home.find(FLOOR, Space.class).orElseThrow().childModels(Space.class).isEmpty());
                    assertEquals(HOME, home.find(LIVING, Space.class).orElseThrow().get().parentId());
                    assertEquals(3, home.find(LIVING, Space.class).orElseThrow().childModels(Device.class).size());
                })
                .andThen().whenCommand(new MoveSpace(LIVING, HOME)).expectNoEvents();
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void movingToAnUnknownDestinationKeepsTheLayout(boolean async) {
        (async ? asyncHouse() : house()).whenCommand(new MoveSpace(LIVING, new HomeId("missing")))
                .expectExceptionalResult(IllegalCommandException.class).expectNoEvents()
                .andThen().whenCommand(new MoveSpace(LIVING, new SpaceId("missing")))
                .expectExceptionalResult(IllegalCommandException.class).expectNoEvents()
                .expectThat(f -> assertEquals(FLOOR, Fluxzero.loadModel(LIVING).get().parentId()));
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void homeAndRoomCanUseTheSameLocalIdentity(boolean async) {
        var home = new HomeId("same-name");
        var room = new SpaceId("same-name");
        var closet = new SpaceId("closet");
        (async ? TestFixture.createAsync() : TestFixture.create())
                .givenCommands(new CreateHome(home, new HomeDetails("Home"), AMSTERDAM),
                        new AddSpace(room, home, new SpaceDetails("Room", SpaceKind.ROOM)))
                .whenCommand(new AddSpace(closet, room, new SpaceDetails("Closet", SpaceKind.ROOM)))
                .expectNoErrors().expectThat(f -> {
                    f.cache().clear();
                    var graph = Fluxzero.loadGraph(home);
                    assertEquals(home, graph.find(room, Space.class).orElseThrow().get().parentId());
                    assertEquals(room, graph.find(closet, Space.class).orElseThrow().get().parentId());
                    assertEquals(3, graph.stream().filter(node -> node.get() instanceof Place).count());
                });
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void duplicateChildCreationKeepsExistingState(boolean async) {
        var resident = new ResidentId("alex");
        (async ? asyncHouse() : house()).givenCommands(new AddResident(resident, HOME, new ResidentDetails("Alex"), HouseholdRole.OWNER))
                .whenCommand(new AddSpace(LIVING, HOME, new SpaceDetails("Replacement", SpaceKind.OUTDOOR)))
                .expectExceptionalResult(Entity.ALREADY_EXISTS_EXCEPTION).expectNoEvents()
                .andThen().whenCommand(new AddDevice(LIGHT, GARDEN, new DeviceDetails("Replacement"), null, Set.of(Capability.POWER), Set.of()))
                .expectExceptionalResult(Entity.ALREADY_EXISTS_EXCEPTION).expectNoEvents()
                .andThen().whenCommand(new AddResident(resident, HOME, new ResidentDetails("Replacement"), HouseholdRole.GUEST))
                .expectExceptionalResult(Entity.ALREADY_EXISTS_EXCEPTION).expectNoEvents()
                .expectThat(f -> {
                    f.cache().clear();
                    assertEquals(FLOOR, Fluxzero.loadModel(LIVING).get().parentId());
                    assertEquals(LIVING, Fluxzero.loadModel(LIGHT).get().spaceId());
                    assertEquals("Alex", Fluxzero.loadModel(resident).get().details().name());
                });
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void spaceCannotMoveToAnotherHome(boolean async) {
        var other = new HomeId("other");
        var otherRoom = new SpaceId("other-room");
        (async ? asyncHouse() : house()).givenCommands(new CreateHome(other, new HomeDetails("Other"), AMSTERDAM),
                        new AddSpace(otherRoom, other, new SpaceDetails("Other room", SpaceKind.ROOM)))
                .whenCommand(new MoveSpace(LIVING, other))
                .expectExceptionalResult(IllegalCommandException.class).expectNoEvents()
                .andThen().whenCommand(new MoveSpace(LIVING, otherRoom))
                .expectExceptionalResult(IllegalCommandException.class).expectNoEvents()
                .expectThat(f -> assertEquals(FLOOR, Fluxzero.loadModel(LIVING).get().parentId()));
    }
    @Test void movingSpaceUpdatesBothBranches() {
        house().whenCommand(new MoveSpace(LIVING, GARDEN)).expectNoErrors()
                .expectThat(f -> {
                    var graph = Fluxzero.loadGraph(HOME);
                    assertTrue(graph.find(FLOOR, Space.class).orElseThrow().childModels(Space.class).isEmpty());
                    assertEquals(LIVING, graph.find(GARDEN, Space.class).orElseThrow().childModels(Space.class).getFirst().id());
                });
    }
    @Test void cyclesAreRejectedWithoutMovingEitherSpace() {
        house().whenCommand(new MoveSpace(FLOOR, LIVING)).expectExceptionalResult().expectNoEvents()
                .expectThat(f -> assertEquals(HOME, Fluxzero.loadModel(FLOOR).get().parentId()));
    }
    @Test void zonesCanOverlapWithoutOwningSpaces() {
        var zone = new io.fluxzero.home.household.api.ZoneId("downstairs");
        house().whenCommand(new DefineZone(zone, HOME, new ZoneDetails("Downstairs"), Set.of(FLOOR, LIVING)))
                .expectNoErrors().expectThat(f -> {
                    assertEquals(2, Fluxzero.loadModel(zone).get().spaces().size());
                    assertEquals(FLOOR, Fluxzero.loadModel(LIVING).get().parentId());
                });
    }
    @Test void residentsHaveTheirOwnPresence() {
        var resident = new ResidentId("alex");
        house().givenCommands(new AddResident(resident, HOME, new ResidentDetails("Alex"), HouseholdRole.OWNER))
                .whenCommand(new ArriveHome(resident)).expectNoErrors()
                .expectThat(f -> assertEquals(Presence.HOME, Fluxzero.loadModel(resident).get().presence()))
                .andThen().whenCommand(new LeaveHome(resident)).expectNoErrors()
                .expectThat(f -> assertEquals(Presence.AWAY, Fluxzero.loadModel(resident).get().presence()));
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void rc10RemovalUpdatesExistingParentWithoutParentId(boolean asynchronous) {
        var fixture = asynchronous ? asyncHouse() : house();
        fixture.givenCommands(new ChoosePrimaryLight(LIVING, LIGHT))
                .whenCommand(new RemoveDevice(LIGHT)).expectOnlyEvents(new RemoveDevice(LIGHT))
                .expectThat(f -> {
                    assertNull(Fluxzero.loadModel(LIGHT).get());
                    assertNull(Fluxzero.loadModel(LIVING).get().primaryLightId());
                    assertEquals(2, Fluxzero.loadGraph(HOME).descendantModels(Device.class).size());
                });
    }
    @Test void deviceAliasesResolveTheSameModel() {
        house().whenExecuting(f -> assertEquals(LIGHT,
                Fluxzero.loadModel("device-label:reading-light", Device.class).get().deviceId())).expectNoErrors();
    }

    @Test void movingDeviceClearsOldPrimaryLight() {
        house().givenCommands(new ChoosePrimaryLight(LIVING, LIGHT))
                .whenCommand(new MoveDevice(LIGHT, GARDEN)).expectNoErrors()
                .expectThat(f -> { assertEquals(GARDEN, Fluxzero.loadModel(LIGHT).get().spaceId());
                    assertNull(Fluxzero.loadModel(LIVING).get().primaryLightId()); });
    }
    @Test void occupiedSpaceCannotBeRemoved() {
        house().whenCommand(new RemoveSpace(LIVING)).expectExceptionalResult(IllegalCommandException.class).expectNoEvents();
    }
    @Test void removingHomeCascadesToItsIndependentChildren() {
        house().whenCommand(new RemoveHome(HOME)).expectNoErrors().expectThat(f -> {
            assertNull(Fluxzero.loadModel(HOME).get()); assertNull(Fluxzero.loadModel(LIVING).get());
            assertNull(Fluxzero.loadModel(LIGHT).get());
        });
    }
    @Test void movingToTheSameRoomPreservesItsPrimaryLight() {
        house().givenCommands(new ChoosePrimaryLight(LIVING, LIGHT))
                .whenCommand(new MoveDevice(LIGHT, LIVING)).expectNoEvents()
                .expectThat(f -> assertEquals(LIGHT, Fluxzero.loadModel(LIVING).get().primaryLightId()));
    }
}
