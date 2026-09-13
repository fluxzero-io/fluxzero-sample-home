package io.fluxzero.home;

import io.fluxzero.common.api.Data;
import io.fluxzero.home.migration.DetailsUpcaster;
import io.fluxzero.home.model.*;
import io.fluxzero.sdk.test.TestFixture;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static io.fluxzero.home.HouseExample.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DetailsSchemaTest {
    private final TestFixture fixture = TestFixture.create().registerCasters(new DetailsUpcaster());

    @Test void oldHomeDocumentRetainsTimeZoneAndMode() {
        fixture.whenUpcasting(old(Home.class, """
                {"homeId":"canal-house","name":"Canal house","timeZone":"Europe/Amsterdam","mode":"HOLIDAY"}
                """)).expectResult(new Home(HOME, new HomeDetails("Canal house"), AMSTERDAM, HomeMode.HOLIDAY));
    }

    @Test void oldSceneDocumentRetainsActivationHistory() {
        fixture.whenUpcasting(old(Scene.class, """
                {"sceneId":"evening","homeId":"canal-house","name":"Evening","actions":[],
                 "activationCount":7,"lastActivatedAt":"2026-09-14T10:00:00Z"}
                """)).expectResult(new Scene(EVENING, HOME, new SceneDetails("Evening"), List.of(), 7, NOW));
    }

    @Test void oldRoutineDocumentRetainsPauseGenerationAndExecutionHistory() {
        fixture.whenUpcasting(old(Routine.class, """
                {"routineId":"bedtime","homeId":"canal-house","name":"Bedtime","sceneId":"evening",
                 "timing":{"kind":"once","at":"2026-09-14T10:00:00Z"},"enabled":false,
                 "generation":9,"executionCount":3,"lastExecutedAt":"2026-09-14T10:00:00Z","problem":"Scene unavailable"}
                """)).expectResult(new Routine(BEDTIME, HOME, new RoutineDetails("Bedtime"), EVENING,
                new RoutineTiming.Once(NOW), false, null, 9, 3, NOW, "Scene unavailable"));
    }

    @Test void oldAutomationDocumentRetainsTriggerCooldownAndDuplicateProtection() {
        fixture.whenUpcasting(old(Automation.class, """
                {"automationId":"reaction","homeId":"canal-house","name":"Away","sceneId":"evening",
                 "trigger":{"kind":"homeBecomes","mode":"AWAY"},"cooldown":"PT5M","enabled":false,
                 "createdAt":"2026-09-14T10:00:00Z","lastProcessedRevision":42,"executionCount":5,
                 "lastExecutedAt":"2026-09-14T10:00:00Z","problem":"Scene unavailable"}
                """)).expectResult(new Automation(REACTION, HOME, new AutomationDetails("Away"), EVENING,
                new AutomationTrigger.HomeBecomes(HomeMode.AWAY), Duration.ofMinutes(5), false, NOW, 42, 5, NOW,
                "Scene unavailable"));
    }

    @Test void repeatedReadsDoNotMutateTheStoredPayloadOrCurrentDetails() {
        var source = old(Space.class, """
                {"spaceId":"living-room","homeId":"canal-house","name":"Living room","kind":"ROOM"}
                """);
        var original = new String(source.getValue(), StandardCharsets.UTF_8);
        var expected = new Space(LIVING, HOME, null, new SpaceDetails("Living room", SpaceKind.ROOM), null);
        fixture.whenUpcasting(source).expectResult(expected);
        fixture.whenUpcasting(source).expectResult(expected);
        assertEquals(original, new String(source.getValue(), StandardCharsets.UTF_8));
        fixture.whenUpcasting(old(Space.class, """
                {"spaceId":"living-room","homeId":"canal-house","details":{"name":"Living room","kind":"ROOM"}}
                """)).expectResult(expected);
    }

    private static Data<byte[]> old(Class<?> type, String json) {
        return new Data<>(json.getBytes(StandardCharsets.UTF_8), type.getName(), 0, Data.JSON_FORMAT);
    }
}
