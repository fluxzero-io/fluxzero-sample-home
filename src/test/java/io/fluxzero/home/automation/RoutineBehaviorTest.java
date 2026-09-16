package io.fluxzero.home.automation;

import io.fluxzero.common.api.scheduling.ScheduleAutoCancelled;
import io.fluxzero.home.automation.api.PauseRoutine;
import io.fluxzero.home.automation.api.PlanRoutine;
import io.fluxzero.home.automation.api.RemoveRoutine;
import io.fluxzero.home.automation.api.ResumeRoutine;
import io.fluxzero.home.automation.api.RunRoutine;
import io.fluxzero.home.automation.api.model.RoutineDetails;
import io.fluxzero.home.automation.api.model.Weekly;
import io.fluxzero.home.devices.api.RemoveDevice;
import io.fluxzero.home.household.api.RemoveHome;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.fluxzero.home.HouseExample.*;
import static org.junit.jupiter.api.Assertions.*;

class RoutineBehaviorTest {
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void executesAtItsDeadlineAndCompletedIntentCannotExecuteAgain(boolean async) {
        var due = NOW.plusSeconds(60);
        (async ? asyncHouse(new RoutineSchedules()) : house(new RoutineSchedules())).givenCommands(evening())
                .whenCommand(once(due)).expectOnlyActiveScheduledCommands(scheduled(1, due))
                .andThen().whenTimeAdvancesTo(due.minusMillis(1)).expectNoEvents().expectOnlyActiveScheduledCommands(scheduled(1, due))
                .andThen().whenTimeAdvancesTo(due).expectNoErrors().expectNoSchedules()
                .expectThat(f -> { assertEvening(); assertNull(Fluxzero.loadModel(BEDTIME).get().nextRun()); })
                .andThen().whenCommand(new RunRoutine(BEDTIME, 1, due)).expectNoEvents().expectNoSchedules()
                .expectThat(f -> assertEvening());
    }
    @Test void pausedRoutineCannotExecuteEvenIfDeliveryWasAlreadyQueued() {
        var due = NOW.plusSeconds(60);
        house(new RoutineSchedules()).givenCommands(evening(), once(due))
                .whenCommand(new PauseRoutine(BEDTIME)).expectNoSchedules().expectNoMetricsLike(ScheduleAutoCancelled.class)
                .andThen().whenTimeAdvancesTo(due).expectNoEvents()
                .andThen().whenCommand(new RunRoutine(BEDTIME, 1, due)).expectNoEvents().expectNoSchedules()
                .expectThat(f -> assertTrue(Fluxzero.loadModel(LIGHT).get().desiredSettings().isEmpty()));
    }
    @Test void revisedDeadlineReplacesOldGeneration() {
        var oldDue = NOW.plusSeconds(30); var newDue = NOW.plusSeconds(90);
        house(new RoutineSchedules()).givenCommands(evening(), once(oldDue))
                .whenCommand(once(newDue)).expectOnlyActiveScheduledCommands(scheduled(2, newDue))
                .andThen().whenTimeAdvancesTo(oldDue).expectNoEvents()
                .andThen().whenCommand(new RunRoutine(BEDTIME, 1, oldDue)).expectNoEvents().expectOnlyActiveScheduledCommands(scheduled(2, newDue))
                .andThen().whenCommand(new RemoveRoutine(BEDTIME)).expectNoErrors().expectOnlyActiveScheduledCommands()
                .<ScheduleAutoCancelled>expectMetric(metric -> metric.deadline() == newDue.toEpochMilli()
                        && metric.scheduleId().equals(RoutineSchedules.scheduleId(BEDTIME).toString()));
    }
    @Test void removingRoutineCancelsItsDeadline() {
        house(new RoutineSchedules()).givenCommands(evening(), once(NOW.plusSeconds(60)))
                .whenCommand(new RemoveRoutine(BEDTIME)).expectNoSchedules()
                .andThen().whenCommand(new RunRoutine(BEDTIME, 1, NOW.plusSeconds(60))).expectNoEvents().expectNoSchedules();
    }
    @Test void earlyDeliveryDoesNotConsumeIntent() {
        var due = NOW.plusSeconds(60);
        house(new RoutineSchedules()).givenCommands(evening(), once(due))
                .whenCommand(new RunRoutine(BEDTIME, 1, due)).expectNoEvents().expectOnlyActiveScheduledCommands(scheduled(1, due));
    }
    @Test void weeklyRoutineResumesAtNextMoment() {
        var timing = new Weekly(Set.of(DayOfWeek.MONDAY), LocalTime.of(20, 0));
        var due = Instant.parse("2026-09-14T18:00:00Z");
        house(new RoutineSchedules()).givenCommands(evening(), new PlanRoutine(BEDTIME, HOME, new RoutineDetails("Monday"), EVENING, timing), new PauseRoutine(BEDTIME))
                .whenCommand(new ResumeRoutine(BEDTIME)).expectOnlyActiveScheduledCommands(scheduled(3, due))
                .andThen().whenTimeAdvancesTo(due).expectNoErrors()
                .expectOnlyActiveScheduledCommands(scheduled(4, due.plus(Duration.ofDays(7))))
                .andThen().whenCommand(new RemoveRoutine(BEDTIME)).expectNoErrors().expectOnlyActiveScheduledCommands()
                .<ScheduleAutoCancelled>expectMetric(metric -> metric.deadline() == due.plus(Duration.ofDays(7)).toEpochMilli()
                        && metric.scheduleId().equals(RoutineSchedules.scheduleId(BEDTIME).toString()));
    }
    @ParameterizedTest
    @ValueSource(longs = {-1, 0})
    void oneOffMustBeStrictlyInTheFuture(long secondsFromNow) {
        house(new RoutineSchedules()).givenCommands(evening()).whenCommand(once(NOW.plusSeconds(secondsFromNow)))
                .expectExceptionalResult(IllegalCommandException.class).expectNoEvents().expectNoSchedules();
    }

    @Test
    void weeklySelectsTheNextChosenDayStrictlyAfterNow() {
        var timing = new Weekly(Set.of(DayOfWeek.WEDNESDAY, DayOfWeek.MONDAY), LocalTime.NOON);
        var wednesday = Instant.parse("2026-09-16T10:00:00Z");
        var monday = Instant.parse("2026-09-21T10:00:00Z");
        house(new RoutineSchedules()).givenCommands(evening())
                .whenCommand(new PlanRoutine(BEDTIME, HOME, new RoutineDetails("Twice a week"), EVENING, timing))
                .expectOnlyActiveScheduledCommands(scheduled(1, wednesday))
                .andThen().whenTimeAdvancesTo(wednesday).expectNoErrors()
                .expectOnlyActiveScheduledCommands(scheduled(2, monday));
    }
    @Test void springClockGapIsSkipped() {
        var timing = new Weekly(Set.of(DayOfWeek.SUNDAY), LocalTime.of(2, 30));
        house(new RoutineSchedules()).givenCommands(evening()).atFixedTime(Instant.parse("2026-03-28T12:00:00Z"))
                .whenCommand(new PlanRoutine(BEDTIME, HOME, new RoutineDetails("Sunday"), EVENING, timing))
                .expectOnlyActiveScheduledCommands(scheduled(1, Instant.parse("2026-04-05T00:30:00Z")));
    }
    @Test void autumnClockOverlapRunsOnlyOnce() {
        var timing = new Weekly(Set.of(DayOfWeek.SUNDAY), LocalTime.of(2, 30));
        var due = Instant.parse("2026-10-25T00:30:00Z");
        house(new RoutineSchedules()).givenCommands(evening()).atFixedTime(Instant.parse("2026-10-24T12:00:00Z"))
                .whenCommand(new PlanRoutine(BEDTIME, HOME, new RoutineDetails("Sunday"), EVENING, timing)).expectOnlyActiveScheduledCommands(scheduled(1, due))
                .andThen().whenTimeAdvancesTo(due).expectNoErrors()
                .expectOnlyActiveScheduledCommands(scheduled(2, Instant.parse("2026-11-01T01:30:00Z")))
                .andThen().whenTimeAdvancesTo(due.plusSeconds(3600)).expectNoEvents()
                .expectThat(f -> assertEvening());
    }

    @Test
    void planningDuringTheAutumnOverlapSkipsItsSecondOccurrence() {
        var timing = new Weekly(Set.of(DayOfWeek.SUNDAY), LocalTime.of(2, 30));
        house(new RoutineSchedules()).givenCommands(evening())
                .atFixedTime(Instant.parse("2026-10-25T00:45:00Z"))
                .whenCommand(new PlanRoutine(BEDTIME, HOME, new RoutineDetails("Sunday"), EVENING, timing))
                .expectOnlyActiveScheduledCommands(scheduled(1, Instant.parse("2026-11-01T01:30:00Z")));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void lateWeeklyExecutionSkipsMissedOccurrences(boolean async) {
        var timing = new Weekly(Set.of(DayOfWeek.MONDAY), LocalTime.of(20, 0));
        var originalDue = Instant.parse("2026-09-14T18:00:00Z");
        (async ? asyncHouse() : house()).givenCommands(evening(),
                        new PlanRoutine(BEDTIME, HOME, new RoutineDetails("Monday"), EVENING, timing))
                .atFixedTime(Instant.parse("2026-09-30T08:00:00Z"))
                .whenCommand(new RunRoutine(BEDTIME, 1, originalDue)).expectNoErrors()
                .expectThat(f -> {
                    assertEvening();
                    var routine = Fluxzero.loadModel(BEDTIME).get();
                    assertEquals(Instant.parse("2026-10-05T18:00:00Z"), routine.nextRun());
                });
    }

    @Test void failedScenePausesRoutineWithAReasonAndNoPartialChanges() {
        var due = NOW.plusSeconds(60);
        house(new RoutineSchedules()).givenCommands(evening(), once(due), new RemoveDevice(HEAT))
                .whenTimeAdvancesTo(due).expectNoErrors().expectNoSchedules().expectThat(f -> {
                    var routine = Fluxzero.loadModel(BEDTIME).get();
                    assertFalse(routine.enabled()); assertNotNull(routine.problem());
                    assertTrue(Fluxzero.loadModel(LIGHT).get().desiredSettings().isEmpty());
                });
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void homeDeletionCancelsDescendantRoutines(boolean async) {
        (async ? asyncHouse(new RoutineSchedules()) : house(new RoutineSchedules())).givenCommands(evening(), once(NOW.plusSeconds(60)))
                .whenCommand(new RemoveHome(HOME)).expectNoErrors().expectNoSchedules();
    }
}
