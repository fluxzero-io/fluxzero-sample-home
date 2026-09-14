package io.fluxzero.home;

import io.fluxzero.home.command.*;
import io.fluxzero.home.model.*;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.tracking.handling.validation.ValidationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.Set;

import static io.fluxzero.home.HouseExample.*;
import static org.junit.jupiter.api.Assertions.*;

class DetailsBehaviorTest {
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void renamePreservesSpaceKindPlacementAndPrimaryLight(boolean async) {
        (async ? asyncHouse() : house()).givenCommands(new ChoosePrimaryLight(LIVING, LIGHT))
                .whenCommand(new RenameSpace(LIVING, "Study")).expectNoErrors()
                .expectThat(f -> {
                    f.cache().clear();
                    assertEquals(new Space(LIVING, FLOOR, new SpaceDetails("Study", SpaceKind.ROOM), LIGHT),
                            Fluxzero.loadModel(LIVING).get());
                });
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void renamePreservesHomeTimeZoneAndMode(boolean async) {
        (async ? asyncHouse() : house()).givenCommands(new ChangeHomeMode(HOME, HomeMode.HOLIDAY))
                .whenCommand(new RenameHome(HOME, "Our home")).expectNoErrors()
                .expectThat(f -> {
                    f.cache().clear();
                    assertEquals(new Home(HOME, new HomeDetails("Our home"), AMSTERDAM, HomeMode.HOLIDAY),
                            Fluxzero.loadModel(HOME).get());
                });
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void everyDetailsInputCascadesValidation(boolean async) {
        var fixture = (async ? asyncHouse() : house()).givenCommands(evening());
        for (String name : new String[]{null, " ", "x".repeat(121)}) {
            Object[] commands = {
                    new CreateHome(new HomeId("invalid"), new HomeDetails(name), AMSTERDAM),
                    new AddSpace(new SpaceId("invalid"), HOME, new SpaceDetails(name, SpaceKind.ROOM)),
                    new AddDevice(new DeviceId("invalid"), LIVING, new DeviceDetails(name), null, Set.of(Capability.POWER), Set.of()),
                    new AddResident(new ResidentId("invalid"), HOME, new ResidentDetails(name), HouseholdRole.OWNER),
                    new DefineZone(new io.fluxzero.home.model.ZoneId("invalid"), HOME, new ZoneDetails(name), Set.of(LIVING)),
                    new DefineScene(EVENING, HOME, new SceneDetails(name), evening().actions()),
                    new PlanRoutine(BEDTIME, HOME, new RoutineDetails(name), EVENING, new Once(NOW.plusSeconds(10))),
                    new DefineAutomation(REACTION, HOME, new AutomationDetails(name), EVENING,
                            new HomeBecomes(HomeMode.AWAY), Duration.ZERO)
            };
            for (Object command : commands) {
                fixture.whenCommand(command).expectExceptionalResult(ValidationException.class).expectNoEvents().andThen();
            }
        }
        fixture.whenCommand(new AddSpace(new SpaceId("invalid"), HOME, new SpaceDetails("Attic", null)))
                .expectExceptionalResult(ValidationException.class).expectNoEvents().andThen();
        Object[] missing = {
                new CreateHome(new HomeId("invalid"), null, AMSTERDAM),
                new AddSpace(new SpaceId("invalid"), HOME, null),
                new AddDevice(new DeviceId("invalid"), LIVING, null, null, Set.of(Capability.POWER), Set.of()),
                new AddResident(new ResidentId("invalid"), HOME, null, HouseholdRole.OWNER),
                new DefineZone(new io.fluxzero.home.model.ZoneId("invalid"), HOME, null, Set.of(LIVING)),
                new DefineScene(EVENING, HOME, null, evening().actions()),
                new PlanRoutine(BEDTIME, HOME, null, EVENING, new Once(NOW.plusSeconds(10))),
                new DefineAutomation(REACTION, HOME, null, EVENING, new HomeBecomes(HomeMode.AWAY), Duration.ZERO)
        };
        for (Object command : missing) {
            fixture.whenCommand(command).expectExceptionalResult(ValidationException.class).expectNoEvents().andThen();
        }
        fixture.whenExecuting(f -> assertEquals(evening().details(), Fluxzero.loadModel(EVENING).get().details()))
                .expectNoErrors();
    }
}
