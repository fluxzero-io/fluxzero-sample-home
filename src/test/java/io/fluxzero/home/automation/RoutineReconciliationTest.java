package io.fluxzero.home.automation;

import io.fluxzero.home.automation.api.PlanRoutine;
import io.fluxzero.home.automation.api.RoutineId;
import io.fluxzero.home.automation.api.model.Once;
import io.fluxzero.home.automation.api.model.Routine;
import io.fluxzero.home.automation.api.model.RoutineDetails;
import io.fluxzero.home.devices.api.AddDevice;
import io.fluxzero.home.devices.api.DeviceId;
import io.fluxzero.home.devices.api.model.Capability;
import io.fluxzero.home.devices.api.model.DeviceDetails;
import io.fluxzero.home.devices.api.model.Power;
import io.fluxzero.home.household.api.AddSpace;
import io.fluxzero.home.household.api.CreateHome;
import io.fluxzero.home.household.api.HomeId;
import io.fluxzero.home.household.api.RemoveHome;
import io.fluxzero.home.household.api.SpaceId;
import io.fluxzero.home.household.api.model.HomeDetails;
import io.fluxzero.home.household.api.model.SpaceDetails;
import io.fluxzero.home.household.api.model.SpaceKind;
import io.fluxzero.home.scenes.api.DefineScene;
import io.fluxzero.home.scenes.api.SceneId;
import io.fluxzero.home.scenes.api.model.OneDevice;
import io.fluxzero.home.scenes.api.model.SceneDetails;
import io.fluxzero.home.scenes.api.model.SwitchPower;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.test.TestFixture;
import io.fluxzero.sdk.tracking.handling.HandleEvent;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class RoutineReconciliationTest {
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void oldCascadeDeletionCannotCancelARoutineInARecreatedHome(boolean async) {
        var home = new HomeId("recreated");
        var space = new SpaceId("room");
        var light = new DeviceId("light");
        var scene = new SceneId("evening");
        var routine = new RoutineId("evening");
        var now = Instant.parse("2026-09-14T10:00:00Z");
        var oldDeletion = new AtomicReference<Graph<Routine>>();
        var schedules = new RoutineSchedules();
        var observer = new Object() {
            @HandleEvent void changed(Graph<Routine> graph) { if (graph.isEmpty()) oldDeletion.set(graph); }
        };
        var fixture = (async ? TestFixture.createAsync(schedules, observer) : TestFixture.create(schedules, observer))
                .atFixedTime(now).withProperty("fluxzero.defaults.version", "2026.09.10");
        Object[] homeCommands = {
                new CreateHome(home, new HomeDetails("Home"), ZoneId.of("Europe/Amsterdam")),
                new AddSpace(space, home, new SpaceDetails("Room", SpaceKind.ROOM)),
                new AddDevice(light, space, new DeviceDetails("Light"), null, Set.of(Capability.POWER), Set.of()),
                new DefineScene(scene, home, new SceneDetails("Evening"), List.of(new SwitchPower(new OneDevice(light), new Power(false))))
        };
        fixture.givenCommands(homeCommands)
                .givenCommands(new PlanRoutine(routine, home, new RoutineDetails("Old"), scene, new Once(now.plusSeconds(60))))
                .whenCommand(new RemoveHome(home)).expectNoErrors().expectNoSchedules().andThen();
        var newDue = now.plus(Duration.ofHours(2));
        fixture.givenCommands(homeCommands)
                .givenCommands(new PlanRoutine(routine, home, new RoutineDetails("New"), scene, new Once(newDue)))
                // Supplemental consumer-replay probe with the actual historical graph captured above.
                .whenExecuting(f -> {
                    assertNotNull(oldDeletion.get());
                    assertEquals(routine, oldDeletion.get().previous().get().routineId());
                    schedules.changed(oldDeletion.get());
                })
                .expectNoErrors().expectOnlySchedules((java.util.function.Predicate<io.fluxzero.sdk.scheduling.Schedule>) s ->
                        s.getScheduleId().equals(RoutineSchedules.scheduleId(routine).toString()) && s.getDeadline().equals(newDue))
                .expectThat(f -> assertEquals(newDue, Fluxzero.loadModel(routine).get().nextRun()));
    }
}
