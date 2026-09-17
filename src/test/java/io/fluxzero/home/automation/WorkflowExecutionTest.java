package io.fluxzero.home.automation;

import io.fluxzero.common.MessageType;
import io.fluxzero.common.api.SerializedMessage;
import io.fluxzero.home.automation.api.DefineAutomation;
import io.fluxzero.home.automation.api.PauseFailedAutomation;
import io.fluxzero.home.automation.api.PauseFailedRoutine;
import io.fluxzero.home.automation.api.ReactToHome;
import io.fluxzero.home.automation.api.RunRoutine;
import io.fluxzero.home.automation.api.model.AutomationDetails;
import io.fluxzero.home.automation.api.model.HomeBecomes;
import io.fluxzero.home.automation.api.model.HomeModeChanged;
import io.fluxzero.home.devices.api.DimLight;
import io.fluxzero.home.devices.api.SetRoomTemperature;
import io.fluxzero.home.household.api.model.HomeMode;
import io.fluxzero.home.scenes.api.ActivateScene;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.common.Message;
import io.fluxzero.sdk.configuration.DefaultFluxzero;
import io.fluxzero.sdk.publishing.DispatchInterceptor;
import io.fluxzero.sdk.test.TestFixture;
import io.fluxzero.sdk.tracking.handling.HandleEvent;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static io.fluxzero.home.HouseExample.*;
import static org.junit.jupiter.api.Assertions.*;

class WorkflowExecutionTest {
    @ParameterizedTest
    @CsvSource({"false, false, false", "false, true, false", "true, false, false", "true, true, false",
            "false, false, true", "false, true, true", "true, false, true", "true, true, true"})
    void failedExecutionRollsBackAndOnlyFunctionalRefusalPauses(boolean async, boolean routine, boolean functional) {
        var failure = new AtomicReference<RuntimeException>();
        var builder = DefaultFluxzero.builder().addDispatchInterceptor(new DispatchInterceptor() {
            @Override
            public Message interceptDispatch(Message message, MessageType type, String topic) {
                return message;
            }

            @Override
            public SerializedMessage modifySerializedMessage(SerializedMessage serialized, Message message,
                                                               MessageType type, String topic) {
                // Refuse the actual event before storage, after Apply. A preflight cannot catch this.
                if (message.getPayload() instanceof SetRoomTemperature && failure.get() != null) throw failure.get();
                return serialized;
            }
        }, MessageType.EVENT);
        var fixture = populate(async ? TestFixture.createAsync(builder) : TestFixture.create(builder));
        var due = NOW.plusSeconds(60);
        Object execution = routine ? new RunRoutine(BEDTIME, 1, due)
                : new ReactToHome(REACTION, new HomeModeChanged(due, HomeMode.HOME, HomeMode.AWAY));
        var result = fixture.givenCommands(evening(), once(due),
                        new DefineAutomation(REACTION, HOME, new AutomationDetails("Leaving home"), EVENING,
                                new HomeBecomes(HomeMode.AWAY), Duration.ofMinutes(5)))
                .atFixedTime(due)
                .given(f -> failure.set(functional ? new IllegalCommandException("Heating is unavailable.")
                        : new IllegalStateException("Storage is unavailable.")))
                .whenCommand(execution);
        if (functional) {
            result.expectSuccessfulResult().expectNoErrors()
                    .expectEvents(routine ? PauseFailedRoutine.class : PauseFailedAutomation.class);
        } else {
            result.expectExceptionalResult(IllegalStateException.class)
                    .expectNoEventsLike(PauseFailedRoutine.class, PauseFailedAutomation.class);
        }
        result.expectNoEventsLike(ActivateScene.class).expectThat(f -> {
            f.cache().clear();
            assertTrue(Fluxzero.loadModel(LIGHT).get().pendingSettings().isEmpty());
            assertTrue(Fluxzero.loadModel(HEAT).get().pendingSettings().isEmpty());
            if (routine) {
                var actual = Fluxzero.loadModel(BEDTIME).get();
                assertEquals(!functional, actual.enabled());
                assertEquals(functional ? null : due, actual.nextRun());
                assertEquals(functional ? 2 : 1, actual.generation());
                assertEquals(functional ? "Heating is unavailable." : null, actual.problem());
            } else {
                var actual = Fluxzero.loadModel(REACTION).get();
                assertEquals(!functional, actual.enabled());
                assertNull(actual.cooldownEndsAt());
                assertEquals(functional ? "Heating is unavailable." : null, actual.problem());
            }
        });
        if (!functional) {
            result.andThen().given(f -> failure.set(null)).whenCommand(execution)
                    .expectNoErrors().expectEvents(new ActivateScene(EVENING), execution)
                    .expectThat(f -> assertEvening());
        }
    }

    @ParameterizedTest
    @CsvSource({"false, false", "false, true", "true, false", "true, true"})
    void deviceChangesAndWorkflowProgressBecomeVisibleTogether(boolean async, boolean routine) {
        var due = NOW.plusSeconds(60);
        var observer = new Object() {
            @HandleEvent
            void changed(DimLight event) {
                if (routine) {
                    var completed = Fluxzero.loadCurrentGraph(BEDTIME).get();
                    assertNull(completed.nextRun());
                    assertFalse(completed.enabled());
                } else {
                    assertEquals(due.plusSeconds(300), Fluxzero.loadCurrentGraph(REACTION).get().cooldownEndsAt());
                }
            }
        };
        (async ? asyncHouse(observer) : house(observer)).givenCommands(evening(), once(due),
                        new DefineAutomation(REACTION, HOME, new AutomationDetails("Leaving home"), EVENING,
                                new HomeBecomes(HomeMode.AWAY), Duration.ofMinutes(5)))
                .atFixedTime(due)
                .whenCommand(routine ? new RunRoutine(BEDTIME, 1, due)
                        : new ReactToHome(REACTION, new HomeModeChanged(due, HomeMode.HOME, HomeMode.AWAY)))
                .expectNoErrors().expectEvents(new ActivateScene(EVENING))
                .expectThat(f -> assertEvening());
    }
}
