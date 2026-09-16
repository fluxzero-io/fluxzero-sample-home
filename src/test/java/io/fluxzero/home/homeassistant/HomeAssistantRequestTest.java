package io.fluxzero.home.homeassistant;

import com.fasterxml.jackson.databind.JsonNode;
import io.fluxzero.home.homeassistant.api.CallHomeAssistantService;
import io.fluxzero.home.homeassistant.api.ConnectHomeAssistant;
import io.fluxzero.home.homeassistant.api.DisconnectHomeAssistant;
import io.fluxzero.home.homeassistant.api.GetHomeAssistantStates;
import io.fluxzero.home.homeassistant.api.GetHomeAssistantTemperatureUnit;
import io.fluxzero.home.homeassistant.api.HomeAssistantId;
import io.fluxzero.home.homeassistant.api.model.HomeAssistantAction;
import io.fluxzero.home.homeassistant.api.model.HomeAssistantConnection;
import io.fluxzero.home.homeassistant.api.model.HomeAssistantDetails;
import io.fluxzero.home.homeassistant.api.model.HomeAssistantSnapshot;
import io.fluxzero.home.homeassistant.api.model.HomeAssistantUnavailable;
import io.fluxzero.home.household.api.CreateHome;
import io.fluxzero.home.household.api.HomeId;
import io.fluxzero.home.household.api.model.HomeDetails;
import io.fluxzero.sdk.test.TestFixture;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;
import io.fluxzero.sdk.web.RedirectPolicy;
import io.fluxzero.sdk.web.WebRequest;
import io.fluxzero.sdk.web.WebRequestSettings;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.fluxzero.home.homeassistant.HomeAssistantStub.BASE_URL;
import static io.fluxzero.home.homeassistant.HomeAssistantStub.TOKEN;
import static org.junit.jupiter.api.Assertions.*;

/** Commands and queries exercise their real handlers; only the external HTTP responses are stubbed. */
class HomeAssistantRequestTest {
    final HomeAssistantStub remote = new HomeAssistantStub();
    final HomeAssistantConnection connection = new HomeAssistantConnection(new HomeAssistantId("contract"), new HomeId("home"),
            new HomeAssistantDetails("Contract example", "contract"), Duration.ofSeconds(10), null);

    TestFixture configured(boolean async) {
        return (async ? TestFixture.createAsync(remote) : TestFixture.create(remote))
                .withProperty("fluxzero.defaults.version", "2026.09.10")
                .withProperty("home-assistant.contract.url", BASE_URL)
                .withProperty("home-assistant.contract.token", TOKEN)
                .givenCommands(new CreateHome(connection.homeId(), new HomeDetails("Home"), ZoneId.of("UTC")),
                        new ConnectHomeAssistant(connection.connectionId(), connection.homeId(),
                                connection.details(), connection.refreshInterval()));
    }

    WebRequest getStates() {
        return WebRequest.get(BASE_URL + "/api/states").header("Authorization", "Bearer " + TOKEN)
                .header("Accept", "application/json").build();
    }

    WebRequest dimLight() {
        return WebRequest.post(BASE_URL + "/api/services/light/turn_on").header("Authorization", "Bearer " + TOKEN)
                .header("Accept", "application/json").contentType("application/json")
                .body(new HomeAssistantAction.DimEntity("light.reading", 25)).build();
    }

    CallHomeAssistantService dim() {
        return new CallHomeAssistantService(connection.connectionId(),
                new HomeAssistantAction("light", "turn_on", new HomeAssistantAction.DimEntity("light.reading", 25)));
    }

    GetHomeAssistantStates states() { return new GetHomeAssistantStates(connection.connectionId()); }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void climateSnapshotsUseAuthenticatedInstallationUnits(boolean async) {
        remote.snapshot = "[{\"entity_id\":\"climate.heating\",\"state\":\"heat\",\"attributes\":{\"supported_features\":1}}]";
        remote.config = "{\"unit_system\":{\"temperature\":\"°F\"}}";
        configured(async).whenQuery(states()).expectNoErrors()
                .expectResult((HomeAssistantSnapshot snapshot) -> "°F".equals(snapshot.temperatureUnit()))
                .expectOnlyWebRequests(getStates(), WebRequest.get(BASE_URL + "/api/config")
                        .header("Authorization", "Bearer " + TOKEN).header("Accept", "application/json").build());
    }

    @ParameterizedTest @ValueSource(strings = {"null", "not-json", "{}", "{\"unit_system\":{\"temperature\":\"K\"}}"})
    void climateNeverGuessesItsTemperatureUnit(String config) {
        remote.config = config;
        configured(false).whenQuery(new GetHomeAssistantTemperatureUnit(connection.connectionId()))
                .expectExceptionalResult(HomeAssistantUnavailable.class);
    }

    @Test
    void configurationRefusalKeepsTheCredentialError() {
        remote.configStatus = 401;
        configured(false).whenQuery(new GetHomeAssistantTemperatureUnit(connection.connectionId()))
                .expectExceptionalResult(HomeAssistantUnavailable.class)
                .verifyExceptionalResult(failure -> assertEquals(
                        "Home Assistant refused access. Check the configured token and permissions.", failure.getMessage()));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void getStatesUsesBearerAndPreservesConfiguredBasePath(boolean async) {
        configured(async).whenQuery(states())
                .expectResult((HomeAssistantSnapshot snapshot) -> snapshot.discover().size() == 3)
                .expectNoErrors().expectOnlyWebRequests(getStates())
                .expectWebRequest(request -> {
                    var settings = request.getMetadata().get("settings", WebRequestSettings.class);
                    return settings.getTimeout().equals(Duration.ofSeconds(5))
                            && settings.getRedirectPolicy() == RedirectPolicy.NEVER;
                });
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void serviceUsesExactJsonAndDoesNotWriteToStates(boolean async) {
        configured(async).whenCommand(dim()).expectSuccessfulResult().expectNoErrors()
                .expectOnlyWebRequests(dimLight()).expectWebRequest(request -> {
                    JsonNode body = request.getPayloadAs(JsonNode.class);
                    return body.size() == 2 && body.path("entity_id").asText().equals("light.reading")
                            && body.path("brightness_pct").asInt() == 25;
                });
    }

    @ParameterizedTest @ValueSource(strings = {"turn_on", "turn_off"})
    void switchServicesSendOnlyTheEntityInTheirRestBody(String service) {
        var action = new HomeAssistantAction("switch", service, new HomeAssistantAction.SwitchEntity("switch.fountain"));
        configured(true).whenCommand(new CallHomeAssistantService(connection.connectionId(), action))
                .expectSuccessfulResult().expectNoErrors()
                .expectOnlyWebRequests(WebRequest.post(BASE_URL + "/api/services/switch/" + service)
                        .header("Authorization", "Bearer " + TOKEN).header("Accept", "application/json")
                        .contentType("application/json").body(action.body()).build())
                .expectWebRequest(request -> {
                    JsonNode body = request.getPayloadAs(JsonNode.class);
                    return body.size() == 1 && body.path("entity_id").asText().equals("switch.fountain");
                });
    }

    @Test
    void readingADisconnectedInstallationDoesNotSendHttp() {
        configured(true).givenCommands(new DisconnectHomeAssistant(connection.connectionId()))
                .whenQuery(states()).expectExceptionalResult(IllegalCommandException.class)
                .expectNoWebRequests();
    }

    @Test
    void callingADisconnectedInstallationDoesNotSendHttp() {
        configured(true).givenCommands(new DisconnectHomeAssistant(connection.connectionId()))
                .whenCommand(dim()).expectExceptionalResult(IllegalCommandException.class)
                .expectNoWebRequests();
    }

    @Test
    void transientReadFailuresRecoverThroughSdkRetries() {
        remote.nextReadStatuses.addAll(List.of(503, 502));
        configured(true).whenQuery(states()).expectSuccessfulResult()
                .expectNoErrors().expectOnlyWebRequests(getStates(), getStates(), getStates());
    }

    @Test
    void transientServiceFailuresRetryTheSameExplicitSetting() {
        remote.nextServiceStatuses.addAll(List.of(503, 504));
        configured(true).whenCommand(dim()).expectSuccessfulResult().expectNoErrors()
                .expectOnlyWebRequests(dimLight(), dimLight(), dimLight());
    }

    @ParameterizedTest @ValueSource(ints = {302, 401, 403, 404})
    void nonRetryableFailuresDoNotCopyRemoteDiagnostics(int code) {
        remote.readStatus = code;
        remote.snapshot = "Sensitive diagnostic and token that must not enter a domain error";
        configured(false).whenQuery(states())
                .expectExceptionalResult(HomeAssistantUnavailable.class)
                .verifyExceptionalResult(failure -> assertEquals(code == 401 || code == 403
                        ? "Home Assistant refused access. Check the configured token and permissions."
                        : "Home Assistant returned HTTP " + code + ".", failure.getMessage()))
                .expectOnlyWebRequests(getStates());
    }

    @Test
    void refusedServiceReturnsAnActionableFailureToItsCaller() {
        remote.serviceStatus = 401;
        configured(true).whenCommand(dim()).expectExceptionalResult(HomeAssistantUnavailable.class)
                .verifyExceptionalResult(failure -> assertEquals(
                        "Home Assistant refused access. Check the configured token and permissions.", failure.getMessage()))
                .expectOnlyWebRequests(dimLight());
    }

    @ParameterizedTest @ValueSource(ints = {502, 503, 504})
    void persistentFailureStopsAfterTheConfiguredAttempts(int status) {
        remote.readStatus = status;
        configured(true).whenQuery(states())
                .expectExceptionalResult(HomeAssistantUnavailable.class)
                .verifyExceptionalResult(failure -> assertEquals("Home Assistant is temporarily unavailable.", failure.getMessage()))
                .expectOnlyWebRequests(getStates(), getStates(), getStates());
    }

    @ParameterizedTest @ValueSource(strings = {"", "not-json", "null", "{\"unexpected\":\"sensitive diagnostic\"}"})
    void malformedSnapshotIsReportedWithoutItsContent(String body) {
        remote.snapshot = body;
        configured(false).whenQuery(states())
                .expectExceptionalResult(HomeAssistantUnavailable.class)
                .verifyExceptionalResult(failure -> assertEquals("Home Assistant returned an invalid state snapshot.", failure.getMessage())).expectOnlyWebRequests(getStates());
    }

    @ParameterizedTest @ValueSource(strings = {"", "/relative", "http:ha", "http:/ha", "ftp://ha", "https://user:token@ha", "https://ha?a=b", "https://ha#part"})
    void rejectsUnsafeOrMalformedConfiguredUrls(String url) {
        configured(false).withProperty("home-assistant.contract.url", url)
                .whenQuery(states()).expectExceptionalResult(HomeAssistantUnavailable.class).expectNoWebRequests();
    }

    @ParameterizedTest @ValueSource(strings = {"", "token\nheader", "invalid token", "非ASCII"})
    void invalidCredentialsAreRejectedBeforePublishing(String token) {
        configured(false).withProperty("home-assistant.contract.token", token)
                .whenQuery(states())
                .expectExceptionalResult(HomeAssistantUnavailable.class)
                .verifyExceptionalResult(failure -> assertEquals("Configure the Home Assistant URL and access token.", failure.getMessage())).expectNoWebRequests();
    }
}
