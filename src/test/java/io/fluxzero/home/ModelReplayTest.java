package io.fluxzero.home;

import io.fluxzero.home.automation.RoutineSchedules;
import io.fluxzero.home.command.*;
import io.fluxzero.home.model.*;
import io.fluxzero.sdk.Fluxzero;
import org.junit.jupiter.api.Test;

import static io.fluxzero.home.HouseExample.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelReplayTest {
    @Test void sceneAndRelationsSurviveCacheFreeReconstruction() {
        house().givenCommands(evening(), new ActivateScene(EVENING))
                .whenExecuting(f -> {
                    f.cache().clear();
                    assertEvening();
                    assertEquals(evening().actions(), Fluxzero.loadModel(EVENING).get().actions());
                    assertEquals(3, Fluxzero.loadGraph(HOME).descendantModels(Device.class).size());
                }).expectNoErrors();
    }

    @Test void rc10ParentUpdateReplaysAfterChildHasBeenRemoved() {
        house().givenCommands(new ChoosePrimaryLight(LIVING, LIGHT), new RemoveDevice(LIGHT))
                .whenExecuting(f -> {
                    f.cache().clear();
                    assertNull(Fluxzero.loadModel(LIGHT).get());
                    assertNull(Fluxzero.loadModel(LIVING).get().primaryLightId());
                }).expectNoErrors();
    }

    @Test void completedRoutineAndSceneReconstructFromStoredHistory() {
        var due = NOW.plusSeconds(10);
        house(new RoutineSchedules()).givenCommands(evening(), once(due))
                .whenTimeAdvancesTo(due).expectNoErrors().expectNoSchedules()
                .andThen().whenExecuting(f -> {
                    f.cache().clear();
                    assertEvening();
                    var routine = Fluxzero.loadModel(BEDTIME).get();
                    assertFalse(routine.enabled());
                    assertNull(routine.nextRun());
                    assertEquals(2, routine.generation());
                }).expectNoErrors();
    }
}
