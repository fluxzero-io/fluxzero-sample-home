package io.fluxzero.home.homeassistant;

import com.sun.net.httpserver.HttpServer;
import io.fluxzero.home.model.HomeId;
import io.fluxzero.sdk.test.TestFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** A local HTTP peer verifies the published REST protocol without accounts, hardware or external network access. */
class HomeAssistantApiTest {
    final HomeAssistantApi api = new HomeAssistantApi();
    final List<Request> requests = new ArrayList<>();
    final HomeAssistantConnection connection = new HomeAssistantConnection(new HomeAssistantId("contract"), new HomeId("home"),
            new HomeAssistantDetails("Contract example", "contract"), Duration.ofSeconds(10), null);
    HttpServer server;
    int status = 200;
    String response = """
            [{"entity_id":"light.reading","state":"on","attributes":{
               "friendly_name":"Reading light","supported_color_modes":["brightness"],"brightness":128},
               "last_changed":"2026-09-14T10:00:00Z","last_updated":"2026-09-14T10:00:00Z"}]
            """;

    @BeforeEach
    void startPeer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.add(new Request(exchange.getRequestMethod(), exchange.getRequestURI().toString(),
                    exchange.getRequestHeaders().getFirst("Authorization"),
                    exchange.getRequestHeaders().getFirst("Content-Type"),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            exchange.getResponseHeaders().set("Location", "/must-not-follow");
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
    }

    @AfterEach void stopPeer() { api.close(); server.stop(0); }

    TestFixture configured() {
        return TestFixture.create().withProperty("home-assistant.contract.url", "http://127.0.0.1:" + server.getAddress().getPort() + "/ha")
                .withProperty("home-assistant.contract.token", "example-token-not-a-secret");
    }

    @Test
    void getStatesUsesBearerAndPreservesConfiguredBasePath() {
        configured().whenApplying(f -> api.states(connection).discover()).expectSuccessfulResult()
                .expectThat(f -> assertEquals(List.of(new Request("GET", "/ha/api/states", "Bearer example-token-not-a-secret", null, "")), requests));
    }

    @Test
    void serviceUsesExactJsonAndDoesNotWriteToStates() {
        configured().whenExecuting(f -> api.call(connection, new HomeAssistantAction("light", "turn_on",
                        new HomeAssistantAction.DimEntity("light.reading", 25))))
                .expectSuccessfulResult().expectThat(f -> assertEquals(List.of(new Request("POST", "/ha/api/services/light/turn_on",
                        "Bearer example-token-not-a-secret", "application/json", "{\"entity_id\":\"light.reading\",\"brightness_pct\":25}")), requests));
    }

    @ParameterizedTest @ValueSource(ints = {302, 401, 403, 500})
    void failuresAreSanitizedAndRedirectsAreNotFollowed(int code) {
        status = code;
        response = "Sensitive diagnostic and token that must never escape";
        configured().whenApplying(f -> assertThrows(HomeAssistantUnavailable.class, () -> api.states(connection)).getMessage())
                .expectResult(code == 401 || code == 403
                        ? "Home Assistant refused access. Check the configured token and permissions."
                        : "Home Assistant returned HTTP " + code + ".")
                .expectThat(f -> assertEquals(1, requests.size()));
    }

    @ParameterizedTest @ValueSource(strings = {"", "{\"unexpected\":\"sensitive diagnostic\"}"})
    void malformedSnapshotIsReportedWithoutItsContent(String body) {
        response = body;
        configured().whenApplying(f -> api.states(connection)).expectExceptionalResult(HomeAssistantUnavailable.class);
    }

    @ParameterizedTest @ValueSource(strings = {"", "/relative", "http:ha", "http:/ha", "ftp://ha", "https://user:token@ha", "https://ha?a=b", "https://ha#part"})
    void rejectsUnsafeOrMalformedConfiguredUrls(String url) {
        configured().withProperty("home-assistant.contract.url", url)
                .whenApplying(f -> api.states(connection)).expectExceptionalResult(HomeAssistantUnavailable.class)
                .expectThat(f -> assertTrue(requests.isEmpty()));
    }

    @ParameterizedTest @ValueSource(strings = {"", "token\nheader", "invalid token", "非ASCII"})
    void invalidCredentialsAreRejectedWithoutSendingOrPrintingThem(String token) {
        configured().withProperty("home-assistant.contract.token", token)
                .whenApplying(f -> assertThrows(HomeAssistantUnavailable.class, () -> api.states(connection)).getMessage())
                .expectResult("Configure the Home Assistant URL and access token.")
                .expectThat(f -> assertTrue(requests.isEmpty()));
    }

    record Request(String method, String path, String authorization, String contentType, String body) {}
}
