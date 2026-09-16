package io.fluxzero.home;

import com.fasterxml.jackson.databind.JsonNode;
import io.fluxzero.home.command.*;
import io.fluxzero.home.automation.HomeReactions;
import io.fluxzero.home.homeassistant.*;
import io.fluxzero.home.model.*;
import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.test.Given;
import io.fluxzero.sdk.test.TestFixture;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;
import io.fluxzero.sdk.web.WebRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static io.fluxzero.home.HouseExample.*;
import static org.junit.jupiter.api.Assertions.*;
import static io.fluxzero.home.homeassistant.HomeAssistantStub.*;

/** Household commands run through the real adapter; only Home Assistant HTTP responses are stubbed. */
class HomeAssistantTest {
    static final HomeAssistantId CONNECTION = new HomeAssistantId("example");
    static final Duration INTERVAL = Duration.ofSeconds(10);
    final HomeAssistantStub remote = new HomeAssistantStub();
    final HomeAssistantIntegration integration = new HomeAssistantIntegration();

    TestFixture connected(boolean async) {
        return (async ? asyncHouse(integration, remote) : house(integration, remote))
                .withProperty("home-assistant.example.url", BASE_URL)
                .withProperty("home-assistant.example.token", TOKEN)
                .givenCommands(new ConnectHomeAssistant(CONNECTION, HOME,
                        new HomeAssistantDetails("Example Home Assistant", "example"), INTERVAL));
    }

    TestFixture linked(boolean async) {
        return connected(async).givenCommands(new LinkHomeAssistantDevice(LIGHT, CONNECTION, Set.of("light.reading")));
    }

    Given<?> awaitingDelivery(boolean async) {
        return linked(async).given(f -> remote.serviceStatus = 503)
                .whenCommand(new DimLight(LIGHT, new LightLevel(20)))
                .expectSuccessfulResult().expectError(HomeAssistantUnavailable.class)
                .expectSchedules(new DeliverHomeAssistantSettings(LIGHT, CONNECTION)).andThen();
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void discoveringShowsSupportedCapabilitiesWithoutImportingAnything(boolean async) {
        connected(async).whenQuery(new DiscoverHomeAssistantDevices(CONNECTION))
                .expectResult(List.of(new HomeAssistantEntity("light.reading", "Reading lamp",
                                Set.of(Capability.POWER, Capability.LIGHT_LEVEL), Set.of()),
                        new HomeAssistantEntity("sensor.temperature", "Temperature", Set.of(), Set.of(Measurement.TEMPERATURE)),
                        new HomeAssistantEntity("binary_sensor.motion", "Motion", Set.of(), Set.of(Measurement.MOTION))))
                .expectNoEvents().expectThat(f -> assertTrue(Fluxzero.loadGraph(CONNECTION).childModels(HomeAssistantDevice.class).isEmpty()));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void committedDimBecomesAServiceCallButNotAnObservation(boolean async) {
        linked(async)
                .whenCommand(new DimLight(LIGHT, new LightLevel(25)))
                .expectNoErrors().expectQueries(new GetHomeAssistantStates(CONNECTION))
                .expectCommands(new CallHomeAssistantService(CONNECTION,
                        new HomeAssistantAction("light", "turn_on", new HomeAssistantAction.DimEntity("light.reading", 25))))
                .expectWebRequests(dimRequest(25)).expectThat(f -> {
                    assertEquals(new LightLevel(25), Fluxzero.loadModel(LIGHT).get().desiredSettings().get(Capability.LIGHT_LEVEL));
                    assertEquals(new LightLevel(0), Fluxzero.loadModel(LIGHT, DeviceStatus.class).get().reportedSettings().get(Capability.LIGHT_LEVEL));
                });
    }

    @Test
    void completeSnapshotCombinesSensorEntitiesAndConvertsUnits() {
        connected(false).whenCommand(new LinkHomeAssistantDevice(SENSOR, CONNECTION,
                        Set.of("sensor.temperature", "binary_sensor.motion")))
                .expectNoErrors().expectThat(f -> {
                    var status = Fluxzero.loadModel(SENSOR, DeviceStatus.class).get();
                    assertEquals(0, new BigDecimal("20").compareTo(status.readings().get(Measurement.TEMPERATURE)));
                    assertEquals(BigDecimal.ZERO, status.readings().get(Measurement.MOTION));
                });
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void heatingUsesASetpointAndWaitsForAnIndependentObservation(boolean async) {
        remote.snapshot = """
                [{"entity_id":"climate.heating","state":"heat","attributes":{
                  "supported_features":1,"min_temp":45,"max_temp":95,"temperature":68,"current_temperature":66}}]
                """;
        remote.config = "{\"unit_system\":{\"temperature\":\"°F\"}}";
        connected(async).givenCommands(new LinkHomeAssistantDevice(HEAT, CONNECTION, Set.of("climate.heating")))
                .whenCommand(new SetRoomTemperature(HEAT, new RoomTemperature(BigDecimal.valueOf(21))))
                .expectNoErrors().expectWebRequest(request -> {
                    var body = request.<JsonNode>getPayloadAs(JsonNode.class);
                    return WebRequest.getUrl(request.getMetadata()).endsWith("/api/services/climate/set_temperature")
                            && body.size() == 2 && body.path("entity_id").asText().equals("climate.heating")
                            && body.path("temperature").decimalValue().compareTo(new BigDecimal("69.8")) == 0;
                }).expectThat(f -> assertEquals(0, BigDecimal.valueOf(20).compareTo(((RoomTemperature)
                        Fluxzero.loadModel(HEAT, DeviceStatus.class).get().reportedSettings().get(Capability.TEMPERATURE)).celsius())))
                .andThen().given(f -> remote.snapshot = remote.snapshot.replace("\"temperature\":68", "\"temperature\":69.8"))
                .whenTimeElapses(INTERVAL).expectNoErrors().expectNoWebRequestLike(r -> r.getMethod().equals("POST"))
                .expectThat(f -> assertEquals(0, BigDecimal.valueOf(21).compareTo(((RoomTemperature)
                        Fluxzero.loadModel(HEAT, DeviceStatus.class).get().reportedSettings().get(Capability.TEMPERATURE)).celsius())));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void coversSendPercentOpenAndObserveActualPosition(boolean async) {
        var blinds = new DeviceId("blinds");
        remote.snapshot = """
                [{"entity_id":"cover.window","state":"open","attributes":{"supported_features":15,"current_position":20}}]
                """;
        connected(async).givenCommands(new AddDevice(blinds, LIVING, new DeviceDetails("Window shades"), null,
                        Set.of(Capability.OPENING), Set.of()), new LinkHomeAssistantDevice(blinds, CONNECTION, Set.of("cover.window")))
                .whenCommand(new SetOpening(blinds, 65))
                .expectNoErrors().expectWebRequest(request -> {
                    var body = request.<JsonNode>getPayloadAs(JsonNode.class);
                    return WebRequest.getUrl(request.getMetadata()).endsWith("/api/services/cover/set_cover_position")
                            && body.size() == 2 && body.path("entity_id").asText().equals("cover.window")
                            && body.path("position").asInt() == 65;
                }).expectThat(f -> assertEquals(new Opening(20),
                        Fluxzero.loadModel(blinds, DeviceStatus.class).get().reportedSettings().get(Capability.OPENING)))
                .andThen().given(f -> remote.snapshot = remote.snapshot.replace("\"current_position\":20", "\"current_position\":65"))
                .whenTimeElapses(INTERVAL).expectNoErrors()
                .expectThat(f -> assertEquals(new Opening(65),
                        Fluxzero.loadModel(blinds, DeviceStatus.class).get().reportedSettings().get(Capability.OPENING)));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void coloredLightsSendHueAndSaturationAndObserveTheReturnedColor(boolean async) {
        var coloredLight = new DeviceId("colored-light");
        remote.snapshot = """
                [{"entity_id":"light.color","state":"on","attributes":{
                  "supported_color_modes":["hs"],"brightness":128,"hs_color":[30,50]}}]
                """;
        connected(async).givenCommands(new AddDevice(coloredLight, LIVING, new DeviceDetails("Colored light"), null,
                        Set.of(Capability.POWER, Capability.LIGHT_LEVEL, Capability.LIGHT_COLOR), Set.of()),
                        new LinkHomeAssistantDevice(coloredLight, CONNECTION, Set.of("light.color")))
                .whenCommand(new SetLightColor(coloredLight, 210, 70))
                .expectNoErrors().expectWebRequest(request -> {
                    var body = request.<JsonNode>getPayloadAs(JsonNode.class);
                    return WebRequest.getUrl(request.getMetadata()).endsWith("/api/services/light/turn_on") && body.size() == 2
                            && body.path("entity_id").asText().equals("light.color")
                            && body.path("hs_color").toString().equals("[210,70]");
                }).expectThat(f -> assertEquals(new LightColor(30, 50),
                        Fluxzero.loadModel(coloredLight, DeviceStatus.class).get().reportedSettings().get(Capability.LIGHT_COLOR)))
                .andThen().given(f -> remote.snapshot = remote.snapshot.replace("[30,50]", "[210,70]"))
                .whenTimeElapses(INTERVAL).expectNoErrors()
                .expectThat(f -> assertEquals(new LightColor(210, 70),
                        Fluxzero.loadModel(coloredLight, DeviceStatus.class).get().reportedSettings().get(Capability.LIGHT_COLOR)));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void externalChangesAreObservedWithoutWritingThemBack(boolean async) {
        linked(async).given(f -> remote.snapshot = snapshot("on", 128, "77", "on"))
                .whenTimeElapses(INTERVAL)
                .expectNoErrors().expectNoWebRequestLike(r -> r.getMethod().equals("POST")).expectThat(f -> {
                    var status = Fluxzero.loadModel(LIGHT, DeviceStatus.class).get();
                    assertEquals(new LightLevel(50), status.reportedSettings().get(Capability.LIGHT_LEVEL));
                    assertTrue(Fluxzero.loadModel(LIGHT).get().desiredSettings().isEmpty());
                });
    }

    @Test
    void unsupportedCapabilitiesAreRejectedBeforeLinking() {
        connected(false).whenCommand(new LinkHomeAssistantDevice(HEAT, CONNECTION, Set.of("light.reading")))
                .expectExceptionalResult(IllegalCommandException.class)
                .expectThat(f -> assertNull(Fluxzero.loadGraph(HEAT).childModels(HomeAssistantDevice.class).stream().findFirst().orElse(null)));
    }

    @Test
    void absentSensorSourcesAreRejected() {
        connected(false).whenCommand(new LinkHomeAssistantDevice(SENSOR, CONNECTION, Set.of("sensor.missing")))
                .expectExceptionalResult(IllegalCommandException.class);
    }

    @Test
    void cannotLinkAcrossHomes() {
        var other = new HomeId("other");
        connected(false).withProperty("home-assistant.other.url", BASE_URL)
                .withProperty("home-assistant.other.token", TOKEN)
                .givenCommands(new CreateHome(other, new HomeDetails("Other home"), AMSTERDAM),
                        new ConnectHomeAssistant(new HomeAssistantId("other"), other,
                                new HomeAssistantDetails("Other HA", "other"), INTERVAL))
                .whenCommand(new LinkHomeAssistantDevice(LIGHT, new HomeAssistantId("other"), Set.of("light.reading")))
                .expectExceptionalResult(IllegalCommandException.class).expectThat(f -> assertNull(Fluxzero.loadGraph(LIGHT).childModels(HomeAssistantDevice.class).stream().findFirst().orElse(null)));
    }

    @Test
    void sameEntityCannotBeLinkedToTwoDevices() {
        var second = new DeviceId("second-light");
        linked(false).givenCommands(new AddDevice(second, LIVING, new DeviceDetails("Second light"), null,
                        Set.of(Capability.POWER), Set.of()))
                .whenCommand(new LinkHomeAssistantDevice(second, CONNECTION, Set.of("light.reading")))
                .expectExceptionalResult().expectThat(f -> assertNull(Fluxzero.loadGraph(second).childModels(HomeAssistantDevice.class).stream().findFirst().orElse(null)));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void retryUsesLatestIntentionAndClearsOnlyDeliveryProblem(boolean async) {
        awaitingDelivery(async).whenCommand(new DimLight(LIGHT, new LightLevel(60)))
                .expectSuccessfulResult().expectError(HomeAssistantUnavailable.class).andThen()
                .given(f -> remote.serviceStatus = 200)
                .whenTimeElapses(INTERVAL)
                .expectNoErrors().expectWebRequests(dimRequest(60))
                .expectNoWebRequestLike(r -> r.getMethod().equals("POST")
                        && !r.getPayloadAs(HomeAssistantAction.DimEntity.class).equals(new HomeAssistantAction.DimEntity("light.reading", 60)))
                .expectThat(f -> {
                    assertNull(Fluxzero.loadGraph(LIGHT).childModels(HomeAssistantDevice.class).stream().findFirst().orElse(null).problem());
                    assertEquals(new LightLevel(0), Fluxzero.loadModel(LIGHT, DeviceStatus.class).get().reportedSettings().get(Capability.LIGHT_LEVEL));
                });
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void disconnectedGatewayDoesNotInventOfflineDeviceReadingsAndRecovers(boolean async) {
        var result = linked(async).given(f -> remote.readStatus = 503)
                .whenTimeElapses(INTERVAL).expectSuccessfulResult().expectError(HomeAssistantUnavailable.class).expectThat(f -> {
                    assertEquals("Home Assistant is temporarily unavailable.", Fluxzero.loadModel(CONNECTION).get().problem());
                    assertEquals(Availability.ONLINE, Fluxzero.loadModel(LIGHT, DeviceStatus.class).get().availability());
                });
        result.andThen().given(f -> { remote.readStatus = 200; remote.snapshot = snapshot("on", 255, "68", "off"); })
                .whenTimeElapses(INTERVAL).expectNoErrors().expectThat(f -> {
                    assertNull(Fluxzero.loadModel(CONNECTION).get().problem());
                    assertEquals(new LightLevel(100), Fluxzero.loadModel(LIGHT, DeviceStatus.class).get().reportedSettings().get(Capability.LIGHT_LEVEL));
                });
    }

    @Test
    void missingEntitiesAreReportedOfflineAndCanReturnWithTheirOldHaState() {
        var result = linked(false).given(f -> remote.snapshot = "[]")
                .whenTimeElapses(INTERVAL).expectNoErrors().expectThat(f ->
                        assertEquals(Availability.OFFLINE, Fluxzero.loadModel(LIGHT, DeviceStatus.class).get().availability()));
        result.andThen().given(f -> remote.snapshot = snapshot("off", 0, "68", "off"))
                .whenTimeElapses(INTERVAL).expectNoErrors().expectThat(f ->
                        assertEquals(Availability.ONLINE, Fluxzero.loadModel(LIGHT, DeviceStatus.class).get().availability()));
    }

    @Test
    void disconnectCancelsRefreshAndPendingDeliveryWithoutDeletingDevices() {
        awaitingDelivery(false).whenCommand(new DisconnectHomeAssistant(CONNECTION)).expectNoErrors().expectNoSchedules()
                .expectThat(f -> {
                    assertNotNull(Fluxzero.loadModel(LIGHT).get());
                    assertNull(Fluxzero.loadGraph(LIGHT).childModels(HomeAssistantDevice.class).stream().findFirst().orElse(null));
                });
    }

    @Test
    void unlinkCancelsPendingDeliveryAndLaterChangesHaveNoEffect() {
        awaitingDelivery(false).givenCommands(new UnlinkHomeAssistantDevice(LIGHT))
                .whenCommand(new DimLight(LIGHT, new LightLevel(40)))
                .expectNoErrors().expectOnlySchedules(new RefreshHomeAssistant(CONNECTION))
                .expectNoWebRequests();
    }

    @Test
    void rejectedDeviceCommandHasNoExternalEffect() {
        linked(false)
                .whenCommand(new SetRoomTemperature(LIGHT, new RoomTemperature(BigDecimal.valueOf(21))))
                .expectExceptionalResult().expectNoWebRequests();
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void observedTemperatureCanActivateAnOrdinaryHouseholdScene(boolean async) {
        linked(async).registerHandlers(new HomeReactions())
                .givenCommands(new LinkHomeAssistantDevice(SENSOR, CONNECTION, Set.of("sensor.temperature", "binary_sensor.motion")),
                        new DefineScene(EVENING, HOME, new SceneDetails("Temperature signal"),
                                List.of(new DimLights(new OneDevice(LIGHT), new LightLevel(30)))),
                        new DefineAutomation(REACTION, HOME, new AutomationDetails("Too warm"), EVENING,
                                new MeasurementCrosses(SENSOR, Measurement.TEMPERATURE, MeasurementCrosses.Direction.RISES_ABOVE,
                                        BigDecimal.valueOf(24)), Duration.ofMinutes(1)))
                .given(f -> remote.snapshot = snapshot("off", 0, "77", "off"))
                .whenTimeElapses(INTERVAL).expectNoErrors().expectEvents(new ActivateScene(EVENING))
                .expectWebRequests(dimRequest(30));
    }

    static WebRequest dimRequest(int level) {
        return WebRequest.post(BASE_URL + "/api/services/light/turn_on")
                .header("Authorization", "Bearer " + TOKEN).header("Accept", "application/json")
                .contentType("application/json").body(new HomeAssistantAction.DimEntity("light.reading", level)).build();
    }
}
