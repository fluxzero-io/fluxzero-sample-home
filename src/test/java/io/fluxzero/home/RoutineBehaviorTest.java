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

class RoutineBehaviorTest {
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void executesExactlyAtItsDeadlineAndIgnoresDuplicateDelivery(boolean async) {
        var due = NOW.plusSeconds(60);
        (async ? asyncHouse(new RoutineSchedules()) : house(new RoutineSchedules())).givenCommands(evening())
                .whenCommand(once(due)).expectOnlySchedules(scheduled(1, due))
                .andThen().whenTimeAdvancesTo(due.minusMillis(1)).expectNoEvents().expectOnlySchedules(scheduled(1, due))
                .andThen().whenTimeAdvancesTo(due).expectNoErrors().expectNoSchedules()
                .expectThat(f -> { assertEvening(); assertEquals(1, Fluxzero.loadModel(BEDTIME).get().executionCount()); })
                .andThen().whenCommand(new RunRoutine(BEDTIME, 1, due)).expectNoEvents().expectNoSchedules()
                .expectThat(f -> assertEquals(1, Fluxzero.loadModel(BEDTIME).get().executionCount()));
    }
    @Test void pausedRoutineCannotExecuteEvenIfDeliveryWasAlreadyQueued() {
        var due = NOW.plusSeconds(60);
        house(new RoutineSchedules()).givenCommands(evening(), once(due))
                .whenCommand(new PauseRoutine(BEDTIME)).expectNoSchedules()
                .andThen().whenTimeAdvancesTo(due).expectNoEvents()
                .andThen().whenCommand(new RunRoutine(BEDTIME, 1, due)).expectNoEvents().expectNoSchedules()
                .expectThat(f -> assertEquals(0, Fluxzero.loadModel(EVENING).get().activationCount()));
    }
    @Test void revisedDeadlineReplacesOldGeneration() {
        var oldDue = NOW.plusSeconds(30); var newDue = NOW.plusSeconds(90);
        house(new RoutineSchedules()).givenCommands(evening(), once(oldDue))
                .whenCommand(once(newDue)).expectOnlySchedules(scheduled(2, newDue))
                .andThen().whenTimeAdvancesTo(oldDue).expectNoEvents()
                .andThen().whenCommand(new RunRoutine(BEDTIME, 1, oldDue)).expectNoEvents().expectOnlySchedules(scheduled(2, newDue));
    }
    @Test void removingRoutineCancelsItsDeadline() {
        house(new RoutineSchedules()).givenCommands(evening(), once(NOW.plusSeconds(60)))
                .whenCommand(new RemoveRoutine(BEDTIME)).expectNoSchedules()
                .andThen().whenCommand(new RunRoutine(BEDTIME, 1, NOW.plusSeconds(60))).expectNoEvents().expectNoSchedules();
    }
    @Test void earlyDeliveryDoesNotConsumeIntent() {
        var due = NOW.plusSeconds(60);
        house(new RoutineSchedules()).givenCommands(evening(), once(due))
                .whenCommand(new RunRoutine(BEDTIME, 1, due)).expectNoEvents().expectOnlySchedules(scheduled(1, due));
    }
    @Test void weeklyRoutineResumesAtNextMoment() {
        var timing = new RoutineTiming.Weekly(Set.of(DayOfWeek.MONDAY), LocalTime.of(20, 0));
        var due = Instant.parse("2026-09-14T18:00:00Z");
        house(new RoutineSchedules()).givenCommands(evening(), new PlanRoutine(BEDTIME, HOME, new RoutineDetails("Monday"), EVENING, timing), new PauseRoutine(BEDTIME))
                .whenCommand(new ResumeRoutine(BEDTIME)).expectOnlySchedules(scheduled(3, due))
                .andThen().whenTimeAdvancesTo(due).expectNoErrors()
                .expectOnlySchedules(scheduled(4, due.plus(Duration.ofDays(7))));
    }
    @Test void invalidPastRoutineCreatesNoWork() {
        house(new RoutineSchedules()).givenCommands(evening()).whenCommand(once(NOW.minusSeconds(1)))
                .expectExceptionalResult(HomeRuleViolation.class).expectNoEvents().expectNoSchedules();
    }
    @Test void springClockGapIsSkipped() {
        var timing = new RoutineTiming.Weekly(Set.of(DayOfWeek.SUNDAY), LocalTime.of(2, 30));
        house(new RoutineSchedules()).givenCommands(evening()).atFixedTime(Instant.parse("2026-03-28T12:00:00Z"))
                .whenCommand(new PlanRoutine(BEDTIME, HOME, new RoutineDetails("Sunday"), EVENING, timing))
                .expectOnlySchedules(scheduled(1, Instant.parse("2026-04-05T00:30:00Z")));
    }
    @Test void autumnClockOverlapRunsOnlyOnce() {
        var timing = new RoutineTiming.Weekly(Set.of(DayOfWeek.SUNDAY), LocalTime.of(2, 30));
        var due = Instant.parse("2026-10-25T00:30:00Z");
        house(new RoutineSchedules()).givenCommands(evening()).atFixedTime(Instant.parse("2026-10-24T12:00:00Z"))
                .whenCommand(new PlanRoutine(BEDTIME, HOME, new RoutineDetails("Sunday"), EVENING, timing)).expectOnlySchedules(scheduled(1, due))
                .andThen().whenTimeAdvancesTo(due).expectNoErrors()
                .expectOnlySchedules(scheduled(2, Instant.parse("2026-11-01T01:30:00Z")))
                .andThen().whenTimeAdvancesTo(due.plusSeconds(3600)).expectNoEvents()
                .expectThat(f -> assertEquals(1, Fluxzero.loadModel(BEDTIME).get().executionCount()));
    }

    @Test void failedScenePausesRoutineWithAReasonAndNoPartialChanges() {
        var due = NOW.plusSeconds(60);
        house(new RoutineSchedules()).givenCommands(evening(), once(due), new RemoveDevice(HEAT))
                .whenTimeAdvancesTo(due).expectNoErrors().expectNoSchedules().expectThat(f -> {
                    var routine = Fluxzero.loadModel(BEDTIME).get();
                    assertFalse(routine.enabled()); assertNotNull(routine.problem()); assertEquals(0, routine.executionCount());
                    assertTrue(Fluxzero.loadModel(LIGHT).get().desiredSettings().isEmpty());
                });
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void homeDeletionCancelsDescendantRoutines(boolean async) {
        (async ? asyncHouse(new RoutineSchedules()) : house(new RoutineSchedules())).givenCommands(evening(), once(NOW.plusSeconds(60)))
                .whenCommand(new RemoveHome(HOME)).expectNoErrors().expectNoSchedules();
    }
}
