package io.fluxzero.home.household;

import io.fluxzero.home.automation.api.DefineAutomation;
import io.fluxzero.home.automation.api.PlanRoutine;
import io.fluxzero.home.automation.api.model.AutomationDetails;
import io.fluxzero.home.automation.api.model.HomeBecomes;
import io.fluxzero.home.automation.api.model.Once;
import io.fluxzero.home.automation.api.model.RoutineDetails;
import io.fluxzero.home.devices.api.AddDevice;
import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.devices.api.model.Capability;
import io.fluxzero.home.devices.api.model.DeviceDetails;
import io.fluxzero.home.household.api.AddResident;
import io.fluxzero.home.household.api.AddSpace;
import io.fluxzero.home.household.api.ChangeHomeMode;
import io.fluxzero.home.household.api.ChoosePrimaryLight;
import io.fluxzero.home.household.api.CreateHome;
import io.fluxzero.home.household.api.DefineZone;
import io.fluxzero.home.household.api.HomeId;
import io.fluxzero.home.household.api.RenameHome;
import io.fluxzero.home.household.api.RenameSpace;
import io.fluxzero.home.household.api.ResidentId;
import io.fluxzero.home.household.api.SpaceId;
import io.fluxzero.home.household.api.model.Home;
import io.fluxzero.home.household.api.model.HomeDetails;
import io.fluxzero.home.household.api.model.HomeMode;
import io.fluxzero.home.household.api.model.HouseholdRole;
import io.fluxzero.home.household.api.model.ResidentDetails;
import io.fluxzero.home.household.api.model.Space;
import io.fluxzero.home.household.api.model.SpaceDetails;
import io.fluxzero.home.household.api.model.SpaceKind;
import io.fluxzero.home.household.api.model.ZoneDetails;
import io.fluxzero.home.scenes.api.DefineScene;
import io.fluxzero.home.scenes.api.model.SceneDetails;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.tracking.handling.validation.ValidationException;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
                    new DefineZone(new io.fluxzero.home.household.api.ZoneId("invalid"), HOME, new ZoneDetails(name), Set.of(LIVING)),
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
                new DefineZone(new io.fluxzero.home.household.api.ZoneId("invalid"), HOME, null, Set.of(LIVING)),
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
