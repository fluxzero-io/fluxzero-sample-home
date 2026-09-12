package io.fluxzero.home.automation;

import io.fluxzero.home.command.*;
import io.fluxzero.home.model.*;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.test.TestFixture;
import io.fluxzero.sdk.tracking.handling.HandleEvent;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RoutineReconciliationTest {
    @Test void oldHomeDeletionCannotCancelARoutineInARecreatedHome() {
        var home = new HomeId("recreated");
        var space = new SpaceId("room");
        var light = new DeviceId("light");
        var scene = new SceneId("evening");
        var routine = new RoutineId("evening");
        var now = Instant.parse("2026-09-14T10:00:00Z");
        var oldDeletion = new AtomicReference<Graph<Home>>();
        var schedules = new RoutineSchedules();
        var fixture = TestFixture.create(schedules, new Object() {
            @HandleEvent void deleted(RemoveHome event, Graph<Home> graph) { oldDeletion.set(graph); }
        }).atFixedTime(now).withProperty("fluxzero.defaults.version", "2026.09.10");
        Object[] homeCommands = {
                new CreateHome(home, "Home", ZoneId.of("Europe/Amsterdam")),
                new AddSpace(space, home, null, "Room", SpaceKind.ROOM),
                new AddDevice(light, space, "Light", null, Set.of(Capability.POWER), Set.of()),
                new DefineScene(scene, home, "Evening", List.of(new SceneAction(new SceneTarget.OneDevice(light), new DeviceSetting.Power(false))))
        };
        fixture.givenCommands(homeCommands)
                .givenCommands(new PlanRoutine(routine, home, "Old", scene, new RoutineTiming.Once(now.plusSeconds(60))))
                .whenCommand(new RemoveHome(home)).expectNoErrors().expectNoSchedules().andThen();
        var newDue = now.plus(Duration.ofHours(2));
        fixture.givenCommands(homeCommands)
                .givenCommands(new PlanRoutine(routine, home, "New", scene, new RoutineTiming.Once(newDue)))
                // Supplemental consumer-replay probe with the actual historical graph captured above.
                .whenExecuting(f -> schedules.removed(new RemoveHome(home), oldDeletion.get()))
                .expectNoErrors().expectOnlySchedules((java.util.function.Predicate<io.fluxzero.sdk.scheduling.Schedule>) s ->
                        s.getScheduleId().equals(RoutineSchedules.scheduleId(routine).toString()) && s.getDeadline().equals(newDue))
                .expectThat(f -> assertEquals(newDue, Fluxzero.loadModel(routine).get().nextRun()));
    }
}
