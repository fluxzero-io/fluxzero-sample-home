package io.fluxzero.home.automation;

import io.fluxzero.home.automation.api.RoutineId;
import io.fluxzero.home.automation.api.RunRoutine;
import io.fluxzero.home.automation.api.model.Routine;
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
        var routine = eventGraph.current().get();
        if (routine == null) return; // Parent ownership cancels work belonging to a deleted routine.
        var id = routine.routineId();
        if (!routine.enabled() || routine.nextRun() == null) {
            Fluxzero.cancelSchedule(scheduleId(id));
        } else {
            Fluxzero.scheduleCommand(new RunRoutine(id, routine.generation(), routine.nextRun()),
                    scheduleId(id), routine.nextRun());
        }
    }
}
