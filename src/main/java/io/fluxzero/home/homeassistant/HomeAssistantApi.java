package io.fluxzero.home.homeassistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;

/** Only the HTTP boundary knows the token. Requests and remote error bodies never enter the Fluxzero log. */
@Component
public class HomeAssistantApi {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private final ObjectMapper json = new ObjectMapper();

    public HomeAssistantSnapshot states(HomeAssistantConnection connection) {
        var body = exchange(connection, "api/states", null);
        try {
            var states = json.readTree(body);
            if (states == null || !states.isArray()) throw new IOException("Expected a state array");
            var result = new ArrayList<HomeAssistantState>();
            for (var state : states) result.add(HomeAssistantState.from(state));
            return new HomeAssistantSnapshot(result);
        } catch (IOException | IllegalArgumentException invalid) {
            throw new HomeAssistantUnavailable("Home Assistant returned an invalid state snapshot.");
        }
    }

    public void call(HomeAssistantConnection connection, HomeAssistantAction action) {
        exchange(connection, "api/services/" + action.domain() + "/" + action.service(), action.body());
        // HTTP success confirms the service call, not the resulting physical state.
    }

    private String exchange(HomeAssistantConnection connection, String path, Object body) {
        var access = HomeAssistantAccess.load(connection);
        try {
            var request = HttpRequest.newBuilder(access.baseUrl.resolve(path)).timeout(Duration.ofSeconds(5))
                    .header("Authorization", "Bearer " + access.token).header("Accept", "application/json");
            if (body == null) request.GET();
            else request.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
            var response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new HomeAssistantUnavailable("Home Assistant refused access. Check the configured token and permissions.");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new HomeAssistantUnavailable("Home Assistant returned HTTP " + response.statusCode() + ".");
            }
            return response.body();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new HomeAssistantUnavailable("The Home Assistant request was interrupted.");
        } catch (IOException failure) {
            throw new HomeAssistantUnavailable("Home Assistant could not be reached or did not respond in time.");
        }
    }

    @PreDestroy
    public void close() { client.close(); }
}
