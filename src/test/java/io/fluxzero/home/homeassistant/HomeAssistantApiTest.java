package io.fluxzero.home.homeassistant;

import com.fasterxml.jackson.databind.JsonNode;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.sdk.test.TestFixture;
import io.fluxzero.sdk.web.RedirectPolicy;
import io.fluxzero.sdk.web.WebRequest;
import io.fluxzero.sdk.web.WebRequestSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.List;

import static io.fluxzero.home.homeassistant.HomeAssistantStub.BASE_URL;
import static io.fluxzero.home.homeassistant.HomeAssistantStub.TOKEN;
import static org.junit.jupiter.api.Assertions.*;

/** Exact gateway requests and controlled remote responses, without a socket server or mocked application adapter. */
class HomeAssistantApiTest {
    final HomeAssistantApi api = new HomeAssistantApi();
    final HomeAssistantStub remote = new HomeAssistantStub();
    final HomeAssistantConnection connection = new HomeAssistantConnection(new HomeAssistantId("contract"), new HomeId("home"),
            new HomeAssistantDetails("Contract example", "contract"), Duration.ofSeconds(10), null);

    TestFixture configured(boolean async) {
        return (async ? TestFixture.createAsync(remote) : TestFixture.create(remote))
                .withProperty("fluxzero.defaults.version", "2026.09.10")
                .withProperty("home-assistant.contract.url", BASE_URL)
                .withProperty("home-assistant.contract.token", TOKEN);
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

    void dim() {
        api.call(connection, new HomeAssistantAction("light", "turn_on", new HomeAssistantAction.DimEntity("light.reading", 25)));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void getStatesUsesBearerAndPreservesConfiguredBasePath(boolean async) {
        configured(async).whenApplying(f -> api.states(connection).discover())
                .expectSuccessfulResult().expectNoErrors().expectOnlyWebRequests(getStates())
                .expectWebRequest(request -> {
                    var settings = request.getMetadata().get("settings", WebRequestSettings.class);
                    return settings.getTimeout().equals(Duration.ofSeconds(5))
                            && settings.getRedirectPolicy() == RedirectPolicy.NEVER;
                });
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void serviceUsesExactJsonAndDoesNotWriteToStates(boolean async) {
        configured(async).whenExecuting(f -> dim()).expectSuccessfulResult().expectNoErrors()
                .expectOnlyWebRequests(dimLight()).expectWebRequest(request -> {
                    JsonNode body = request.getPayloadAs(JsonNode.class);
                    return body.size() == 2 && body.path("entity_id").asText().equals("light.reading")
                            && body.path("brightness_pct").asInt() == 25;
                });
    }

    @Test
    void transientReadFailuresRecoverThroughSdkRetries() {
        remote.nextReadStatuses.addAll(List.of(503, 502));
        configured(true).whenApplying(f -> api.states(connection).discover()).expectSuccessfulResult()
                .expectNoErrors().expectOnlyWebRequests(getStates(), getStates(), getStates());
    }

    @Test
    void transientServiceFailuresRetryTheSameExplicitSetting() {
        remote.nextServiceStatuses.addAll(List.of(503, 504));
        configured(true).whenExecuting(f -> dim()).expectSuccessfulResult().expectNoErrors()
                .expectOnlyWebRequests(dimLight(), dimLight(), dimLight());
    }

    @ParameterizedTest @ValueSource(ints = {302, 401, 403, 404})
    void nonRetryableFailuresDoNotCopyRemoteDiagnostics(int code) {
        remote.readStatus = code;
        remote.snapshot = "Sensitive diagnostic and token that must not enter a domain error";
        configured(false).whenApplying(f -> assertThrows(HomeAssistantUnavailable.class, () -> api.states(connection)).getMessage())
                .expectResult(code == 401 || code == 403
                        ? "Home Assistant refused access. Check the configured token and permissions."
                        : "Home Assistant returned HTTP " + code + ".")
                .expectOnlyWebRequests(getStates());
    }

    @Test
    void persistentFailureStopsAfterTheConfiguredAttempts() {
        remote.readStatus = 503;
        configured(true).whenApplying(f -> assertThrows(HomeAssistantUnavailable.class, () -> api.states(connection)).getMessage())
                .expectResult("Home Assistant returned HTTP 503.")
                .expectOnlyWebRequests(getStates(), getStates(), getStates());
    }

    @ParameterizedTest @ValueSource(strings = {"", "not-json", "null", "{\"unexpected\":\"sensitive diagnostic\"}"})
    void malformedSnapshotIsReportedWithoutItsContent(String body) {
        remote.snapshot = body;
        configured(false).whenApplying(f -> assertThrows(HomeAssistantUnavailable.class, () -> api.states(connection)).getMessage())
                .expectResult("Home Assistant returned an invalid state snapshot.").expectOnlyWebRequests(getStates());
    }

    @ParameterizedTest @ValueSource(strings = {"", "/relative", "http:ha", "http:/ha", "ftp://ha", "https://user:token@ha", "https://ha?a=b", "https://ha#part"})
    void rejectsUnsafeOrMalformedConfiguredUrls(String url) {
        configured(false).withProperty("home-assistant.contract.url", url)
                .whenApplying(f -> api.states(connection)).expectExceptionalResult(HomeAssistantUnavailable.class).expectNoWebRequests();
    }

    @ParameterizedTest @ValueSource(strings = {"", "token\nheader", "invalid token", "非ASCII"})
    void invalidCredentialsAreRejectedBeforePublishing(String token) {
        configured(false).withProperty("home-assistant.contract.token", token)
                .whenApplying(f -> assertThrows(HomeAssistantUnavailable.class, () -> api.states(connection)).getMessage())
                .expectResult("Configure the Home Assistant URL and access token.").expectNoWebRequests();
    }
}
