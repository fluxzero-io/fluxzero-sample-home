package io.fluxzero.home;

import io.fluxzero.home.model.*;
import io.fluxzero.home.command.*;
import io.fluxzero.home.query.*;
import io.fluxzero.home.automation.*;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.modeling.*;
import io.fluxzero.sdk.test.*;
import io.fluxzero.sdk.scheduling.Schedule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import static io.fluxzero.home.HouseExample.*;
import static org.junit.jupiter.api.Assertions.*;

class HomeModelTest {
    @Test void hierarchyAndIndependentQueries() {
        house().whenQuery(new GetHome(HOME)).expectResult((Graph<Home> graph) -> {
            assertEquals(2, graph.childModels(Space.class).size());
            assertEquals(3, graph.descendantModels(Space.class).size());
            assertEquals(3, graph.descendantModels(Device.class).size());
            assertEquals(HOME, graph.find(LIGHT, Device.class).orElseThrow().ancestor(Home.class).orElseThrow().get().homeId());
            return true;
        }).andThen().whenQuery(new FindDevices(HOME, Capability.LIGHT_LEVEL))
            .expectResult((List<Device> devices) -> devices.size() == 1 && devices.getFirst().deviceId().equals(LIGHT));
    }
    @Test void invalidNameCannotCreateHome() {
        TestFixture.create().whenCommand(new CreateHome(HOME, " ", AMSTERDAM))
                .expectExceptionalResult(HomeRuleViolation.class).expectNoEvents();
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void duplicateCreationCannotOverwriteHome(boolean async) {
        (async ? asyncHouse() : house()).whenCommand(new CreateHome(HOME, "Another name", AMSTERDAM))
                .expectExceptionalResult(Entity.ALREADY_EXISTS_EXCEPTION).expectNoEvents()
                .expectThat(f -> assertEquals("Canal house", Fluxzero.loadModel(HOME).get().name()));
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void optionalEnclosingSpaceMustExistWhenSpecified(boolean async) {
        var fixture = async ? asyncHouse() : house();
        fixture.whenCommand(new AddSpace(new SpaceId("attic"), HOME, null, "Attic", SpaceKind.ROOM))
                .expectNoErrors().expectThat(f -> assertNull(Fluxzero.loadModel(new SpaceId("attic")).get().enclosingSpaceId()))
                .andThen().whenCommand(new AddSpace(new SpaceId("closet"), HOME, new SpaceId("missing"), "Closet", SpaceKind.ROOM))
                .expectExceptionalResult(HomeRuleViolation.class).expectNoEvents()
                .expectThat(f -> assertNull(Fluxzero.loadModel(new SpaceId("closet")).get()));
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void duplicateChildCreationKeepsExistingState(boolean async) {
        var resident = new ResidentId("alex");
        (async ? asyncHouse() : house()).givenCommands(new AddResident(resident, HOME, "Alex", HouseholdRole.OWNER))
                .whenCommand(new AddSpace(LIVING, HOME, null, "Replacement", SpaceKind.OUTDOOR))
                .expectExceptionalResult(Entity.ALREADY_EXISTS_EXCEPTION).expectNoEvents()
                .andThen().whenCommand(new AddDevice(LIGHT, GARDEN, "Replacement", null, Set.of(Capability.POWER), Set.of()))
                .expectExceptionalResult(Entity.ALREADY_EXISTS_EXCEPTION).expectNoEvents()
                .andThen().whenCommand(new AddResident(resident, HOME, "Replacement", HouseholdRole.GUEST))
                .expectExceptionalResult(Entity.ALREADY_EXISTS_EXCEPTION).expectNoEvents()
                .expectThat(f -> {
                    f.cache().clear();
                    assertEquals(FLOOR, Fluxzero.loadModel(LIVING).get().enclosingSpaceId());
                    assertEquals(LIVING, Fluxzero.loadModel(LIGHT).get().spaceId());
                    assertEquals("Alex", Fluxzero.loadModel(resident).get().name());
                });
    }
    @Test void spaceCannotHaveAParentFromAnotherHome() {
        var other = new HomeId("other");
        house().givenCommands(new CreateHome(other, "Other", AMSTERDAM))
                .whenCommand(new AddSpace(new SpaceId("foreign"), other, FLOOR, "Foreign", SpaceKind.ROOM))
                .expectExceptionalResult(HomeRuleViolation.class).expectNoEvents();
    }
    @Test void movingSpaceUpdatesBothBranches() {
        house().whenCommand(new MoveSpace(LIVING, GARDEN)).expectNoErrors()
                .expectThat(f -> {
                    var graph = Fluxzero.loadGraph(HOME);
                    assertTrue(graph.find(FLOOR, Space.class).orElseThrow().childModels(Space.class).isEmpty());
                    assertEquals(LIVING, graph.find(GARDEN, Space.class).orElseThrow().childModels(Space.class).getFirst().spaceId());
                });
    }
    @Test void cyclesAreRejectedWithoutMovingEitherSpace() {
        house().whenCommand(new MoveSpace(FLOOR, LIVING)).expectExceptionalResult().expectNoEvents()
                .expectThat(f -> assertNull(Fluxzero.loadModel(FLOOR).get().enclosingSpaceId()));
    }
    @Test void zonesCanOverlapWithoutOwningSpaces() {
        var zone = new io.fluxzero.home.model.ZoneId("downstairs");
        house().whenCommand(new DefineZone(zone, HOME, "Downstairs", Set.of(FLOOR, LIVING)))
                .expectNoErrors().expectThat(f -> {
                    assertEquals(2, Fluxzero.loadModel(zone).get().spaces().size());
                    assertEquals(FLOOR, Fluxzero.loadModel(LIVING).get().enclosingSpaceId());
                });
    }
    @Test void residentsHaveTheirOwnPresence() {
        var resident = new ResidentId("alex");
        house().givenCommands(new AddResident(resident, HOME, "Alex", HouseholdRole.OWNER))
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
        house().whenCommand(new RemoveSpace(LIVING)).expectExceptionalResult(HomeRuleViolation.class).expectNoEvents();
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
