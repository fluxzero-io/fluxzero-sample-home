package io.fluxzero.home;

import io.fluxzero.common.Guarantee;
import io.fluxzero.common.api.scheduling.ScheduleAutoCancelled;
import io.fluxzero.home.automation.RoutineSchedules;
import io.fluxzero.home.automation.RunRoutine;
import io.fluxzero.home.command.RemoveHome;
import io.fluxzero.home.command.RemoveRoutine;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.scheduling.Schedule;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.atomic.AtomicReference;

import static io.fluxzero.home.HouseExample.*;
import static org.junit.jupiter.api.Assertions.*;

class RoutineOwnershipTest {
    @ParameterizedTest
    @CsvSource({"false, false", "true, false", "false, true", "true, true"})
    void deletionCancelsOwnedExecutionWithoutRoutineConsumer(boolean async, boolean removeHome) {
        var due = NOW.plusSeconds(60);
        var stored = new AtomicReference<Schedule>();
        // Deliberately omit RoutineSchedules: only SDK parent ownership can perform cleanup.
        (async ? asyncHouse() : house()).givenCommands(evening(), once(due))
                .given(fc -> {
                    fc.messageScheduler().scheduleCommand(
                            new Schedule(new RunRoutine(BEDTIME, 1, due), RoutineSchedules.scheduleId(BEDTIME).toString(), due),
                            false, Guarantee.STORED).join();
                    stored.set(fc.messageScheduler().getSchedule(RoutineSchedules.scheduleId(BEDTIME)).orElseThrow());
                })
                .whenCommand(removeHome ? new RemoveHome(HOME) : new RemoveRoutine(BEDTIME))
                .expectSuccessfulResult().expectNoErrors().expectOnlyActiveScheduledCommands()
                .<ScheduleAutoCancelled>expectMetric(metric ->
                        metric.scheduleId().equals(stored.get().getScheduleId())
                        && metric.messageId().equals(stored.get().getMessageId())
                        && metric.deadline() == due.toEpochMilli())
                .andThen().whenTimeAdvancesTo(due).expectNoCommands().expectNoEvents().expectNoErrors();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void oldStoredExecutionCannotReplaceWorkForRecreatedRoutine(boolean async) {
        var oldDue = NOW.plusSeconds(60);
        var newDue = NOW.plusSeconds(120);
        var old = new AtomicReference<Schedule>();
        (async ? asyncHouse() : house()).givenCommands(evening(), once(oldDue))
                .given(fc -> {
                    fc.messageScheduler().scheduleCommand(
                            new Schedule(new RunRoutine(BEDTIME, 1, oldDue), RoutineSchedules.scheduleId(BEDTIME).toString(), oldDue),
                            false, Guarantee.STORED).join();
                    old.set(fc.messageScheduler().getSchedule(RoutineSchedules.scheduleId(BEDTIME)).orElseThrow());
                })
                .whenCommand(new RemoveRoutine(BEDTIME)).expectNoErrors().expectOnlyActiveScheduledCommands()
                .andThen().givenCommands(once(newDue))
                .given(fc -> fc.messageScheduler().scheduleCommand(
                        new Schedule(new RunRoutine(BEDTIME, 1, newDue), RoutineSchedules.scheduleId(BEDTIME).toString(), newDue),
                        false, Guarantee.STORED).join())
                .whenExecuting(fc -> assertThrows(RuntimeException.class,
                        () -> fc.messageScheduler().schedule(old.get(), false, Guarantee.STORED).join()))
                .expectNoErrors().expectNoMetricsLike(ScheduleAutoCancelled.class)
                .expectOnlyActiveScheduledCommands(new RunRoutine(BEDTIME, 1, newDue))
                .andThen().whenTimeAdvancesTo(oldDue).expectNoCommands().expectNoEvents().expectNoErrors()
                .expectOnlyActiveScheduledCommands(new RunRoutine(BEDTIME, 1, newDue))
                .andThen().whenTimeAdvancesTo(newDue).expectNoErrors().expectOnlyActiveScheduledCommands()
                .expectThat(fc -> {
                    assertEvening();
                    assertNull(Fluxzero.loadModel(BEDTIME).get().nextRun());
                });
    }
}
