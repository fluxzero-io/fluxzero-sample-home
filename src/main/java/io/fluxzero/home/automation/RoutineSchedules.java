package io.fluxzero.home.automation;

import io.fluxzero.home.model.Routine;
import io.fluxzero.home.model.RoutineId;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.modeling.Graph;
import io.fluxzero.sdk.scheduling.ScheduleId;
import io.fluxzero.sdk.tracking.Consumer;
import io.fluxzero.sdk.tracking.ThrowingErrorHandler;
import io.fluxzero.sdk.tracking.handling.HandleEvent;
import org.springframework.stereotype.Component;

/** Reconciles scheduled work after commit, using current state even when an old event is redelivered. */
@Component
@Consumer(name = "home-routine-schedules", singleTracker = true, errorHandler = ThrowingErrorHandler.class)
public class RoutineSchedules {
    public static ScheduleId scheduleId(RoutineId id) { return ScheduleId.of("home-routine", id); }
    @HandleEvent
    void changed(Graph<Routine> eventGraph) {
        reconcile(new RoutineId(eventGraph.functionalId()));
    }

    private void reconcile(RoutineId id) {
        var routine = Fluxzero.loadCurrentGraph(id).get();
        if (routine == null) return; // Parent ownership cancels work belonging to a deleted routine.
        if (!routine.enabled() || routine.nextRun() == null) {
            Fluxzero.cancelSchedule(scheduleId(id));
        } else {
            Fluxzero.scheduleCommand(new RunRoutine(id, routine.generation(), routine.nextRun()),
                    scheduleId(id), routine.nextRun());
        }
    }
}
