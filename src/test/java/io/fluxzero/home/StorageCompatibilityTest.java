package io.fluxzero.home;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fluxzero.common.api.modeling.*;
import io.fluxzero.common.api.search.GetDocument;
import io.fluxzero.common.serialization.JsonUtils;
import io.fluxzero.home.automation.HomeReactions;
import io.fluxzero.home.command.*;
import io.fluxzero.home.migration.DetailsUpcaster;
import io.fluxzero.home.model.*;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.persisting.eventsourcing.EventSourcingException;
import io.fluxzero.sdk.test.TestFixture;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.fluxzero.home.HouseExample.AMSTERDAM;
import static io.fluxzero.home.HouseExample.NOW;
import static org.junit.jupiter.api.Assertions.*;

/** Captured rc.11 commits exercise real old events, direct status documents and component documents. */
class StorageCompatibilityTest {
    private static final HomeId HOME = new HomeId("legacy");
    private static final SpaceId ROOM = new SpaceId("legacy-room");
    private static final DeviceId SENSOR = new DeviceId("legacy-sensor");
    private static final DeviceStatusId STATUS = new DeviceStatusId("legacy-sensor");
    private static final SceneId SCENE = new SceneId("legacy-evening");
    private static final RoutineId ROUTINE = new RoutineId("legacy-routine");
    private static final AutomationId AUTOMATION = new AutomationId("legacy-automation");

    private static List<CommitModels> oldCommits(boolean omitLatestStatusEvent) {
        JsonNode records = JsonUtils.fromFile("/legacy/rc11-commits.json", JsonNode.class);
        return java.util.stream.StreamSupport.stream(records.spliterator(), false).map(node -> {
            if (omitLatestStatusEvent) {
                node.path("substeps").forEach(step -> step.path("targets").forEach(target -> {
                    if (target.path("modelType").asText().equals("DeviceStatus")
                            && target.path("expectedSequenceNumber").asLong() == 0) {
                        ((ObjectNode) target).put("storeEvent", false);
                    }
                }));
            }
            ((ObjectNode) node).put("@type", node.has("readRelationships") ? "commitModelsWithRelationships" : "commitModels");
            return JsonUtils.convertValue(node, node.has("readRelationships")
                    ? CommitModelsWithRelationships.class : CommitModels.class);
        }).toList();
    }

    private TestFixture legacy(boolean async, boolean omitLatestStatusEvent) {
        return (async ? TestFixture.createAsync() : TestFixture.create())
                .registerCasters(new DetailsUpcaster()).atFixedTime(NOW)
                .withProperty("fluxzero.defaults.version", "2026.09.10")
                .given(fc -> {
                    for (var commit : oldCommits(omitLatestStatusEvent)) {
                        var result = fc.client().getEventStoreClient().commitModels(commit).join();
                        assertTrue(result.getConflicts().isEmpty(), () -> "Old commit rejected: " + result.getConflicts());
                        assertNull(result.getRebaseStateIndex());
                    }
                    fc.cache().clear();
                });
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void originalCommandEventsReconstructEveryModelWithDetails(boolean async) {
        legacy(async, false).whenExecuting(fc -> {
            assertEquals(new Home(HOME, new HomeDetails("Old house"), AMSTERDAM, HomeMode.HOME), Fluxzero.loadModel(HOME).get());
            assertEquals(new Space(ROOM, HOME, null, new SpaceDetails("Old room", SpaceKind.ROOM), null), Fluxzero.loadModel(ROOM).get());
            assertEquals(new Device(SENSOR, ROOM, new DeviceDetails("Old sensor"), "old-sensor", Set.of(Capability.POWER),
                    Set.of(Measurement.TEMPERATURE), Map.of()), Fluxzero.loadModel(SENSOR).get());
            assertEquals(new Resident(new ResidentId("legacy-alex"), HOME, new ResidentDetails("Alex"), HouseholdRole.OWNER,
                    Presence.UNKNOWN), Fluxzero.loadModel(new ResidentId("legacy-alex")).get());
            assertEquals(new Zone(new ZoneId("legacy-zone"), HOME, new ZoneDetails("Downstairs"), Set.of(ROOM)),
                    Fluxzero.loadModel(new ZoneId("legacy-zone")).get());
            assertEquals(new Scene(SCENE, HOME, new SceneDetails("Evening"), List.of(new SceneAction(new SceneTarget.OneDevice(SENSOR),
                    new DeviceSetting.Power(false))), 0, null), Fluxzero.loadModel(SCENE).get());
            assertEquals(new Routine(ROUTINE, HOME, new RoutineDetails("Evening routine"), SCENE,
                    new RoutineTiming.Once(NOW.plusSeconds(3600)), true, NOW.plusSeconds(3600), 1, 0, null, null),
                    Fluxzero.loadModel(ROUTINE).get());
            assertEquals(new Automation(AUTOMATION, HOME, new AutomationDetails("Too warm"), SCENE,
                    new AutomationTrigger.MeasurementCrosses(SENSOR, Measurement.TEMPERATURE,
                            AutomationTrigger.Direction.RISES_ABOVE, new BigDecimal("24")), Duration.ZERO, true, NOW,
                    -1, 0, null, null), Fluxzero.loadModel(AUTOMATION).get());
            assertEquals(1, Fluxzero.loadGraph(HOME).descendantModels(Device.class).size());
        }).expectNoErrors().expectNoEvents();
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void oldComponentDocumentsUpcastAndRemainSearchableByExistingRelations(boolean async) {
        legacy(async, false).whenExecuting(fc -> {
            for (var commit : oldCommits(false)) {
                for (var step : commit.getSubsteps()) {
                    for (var target : step.getTargets()) {
                        if (target.getDocument() == null || target.getModelType().equals("DeviceStatus")) continue;
                        var stored = target.getDocument().getDocument();
                        var decoded = ((io.fluxzero.sdk.persisting.search.DocumentSerializer) fc.serializer()).fromDocument(stored);
                        var replayed = fc.modelRepository().load(target.getModelId(), decoded.getClass()).get();
                        assertEquals(replayed, decoded, target.getModelType());
                    }
                }
            }
            var devices = Fluxzero.search(Device.class).whereAncestor(HOME).fetchAll();
            assertEquals(List.of(Fluxzero.loadModel(SENSOR).get()), devices);
            assertEquals(List.of(Fluxzero.loadModel(AUTOMATION).get()),
                    Fluxzero.search(Automation.class).whereParent(HOME).fetchAll());
        }).expectNoErrors().expectNoEvents();
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void documentOnlyStatusAlreadyHasDurableHistoryAndNeedsNoRewriting(boolean async) {
        legacy(async, false).whenExecuting(fc -> {
            var before = statusStream(fc);
            assertTrue(before.getHead().isHistoryComplete());
            assertEquals(2, before.getMemberships().size());
            var original = fc.client().getSearchClient().fetchModelDocument(new GetDocument(STATUS.toString(), "DeviceStatus", true));
            assertNotNull(original.getDocument(), "This must start with the old authoritative document");
            var current = new DeviceStatus(STATUS, SENSOR, NOW.minusSeconds(1), Availability.ONLINE,
                    Map.of(Capability.POWER, new DeviceSetting.Power(true)), Map.of(Measurement.TEMPERATURE, new BigDecimal("23")));
            assertEquals(current, ((io.fluxzero.sdk.persisting.search.DocumentSerializer) fc.serializer()).fromDocument(original.getDocument()));
            for (int attempt = 0; attempt < 2; attempt++) {
                fc.cache().clear();
                var graph = Fluxzero.loadGraph(STATUS);
                assertEquals(current, graph.get());
                assertEquals(new BigDecimal("22"), graph.previous().get().readings().get(Measurement.TEMPERATURE));
                assertEquals(NOW.minusSeconds(2), graph.previous().get().observedAt());
                assertEquals(HOME, graph.ancestor(Home.class).orElseThrow().get().homeId());
                assertEquals(before, statusStream(fc), "Reading the upgraded model must not append events or move revisions");
            }
            assertEquals(original.getDocument(), fc.client().getSearchClient()
                    .fetchModelDocument(new GetDocument(STATUS.toString(), "DeviceStatus", true)).getDocument());
        }).expectNoErrors().expectNoEvents();
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void firstNewObservationUsesOldEvidenceAndActivatesOnlyOnce(boolean async) {
        var fixture = legacy(async, false).registerHandlers(new HomeReactions());
        var report = new ReportDeviceStatus(STATUS, SENSOR, NOW, Availability.ONLINE,
                Map.of(Capability.POWER, new DeviceSetting.Power(true)), Map.of(Measurement.TEMPERATURE, new BigDecimal("25")));
        fixture.whenCommand(report).expectNoErrors().expectThat(fc -> {
            assertEquals(1, Fluxzero.loadModel(SCENE).get().activationCount());
            assertEquals(1, Fluxzero.loadModel(AUTOMATION).get().executionCount());
            assertEquals(new DeviceSetting.Power(false), Fluxzero.loadModel(SENSOR).get().desiredSettings().get(Capability.POWER));
            fc.cache().clear();
            assertEquals(new BigDecimal("23"), Fluxzero.loadGraph(STATUS).previous().get().readings().get(Measurement.TEMPERATURE));
        }).andThen().whenCommand(report).expectNoErrors().expectNoEvents()
                .expectThat(fc -> assertEquals(1, Fluxzero.loadModel(SCENE).get().activationCount()));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void missingHistoryIsRejectedInsteadOfInventingABaseline(boolean async) {
        legacy(async, true).whenExecuting(fc -> {
            assertFalse(statusStream(fc).getHead().isHistoryComplete());
            assertNotNull(fc.client().getSearchClient().fetchModelDocument(new GetDocument(STATUS.toString(), "DeviceStatus", true)).getDocument());
            assertThrows(EventSourcingException.class, () -> Fluxzero.loadModel(STATUS).get());
        }).expectNoErrors().expectNoEvents();
    }

    private static ModelEventStream statusStream(Fluxzero fc) {
        return fc.client().getEventStoreClient().getModelEvents(new GetModelEvents(
                List.of(new ModelEventStreamRequest(STATUS.toString(), -1, 100)), ModelReadBoundary.current(), 0))
                .getStreams().getFirst();
    }
}
