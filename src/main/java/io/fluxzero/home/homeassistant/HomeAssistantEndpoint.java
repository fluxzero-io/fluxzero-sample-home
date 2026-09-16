package io.fluxzero.home.homeassistant;

import io.fluxzero.sdk.Fluxzero;
import io.fluxzero.sdk.configuration.ApplicationProperties;
import io.fluxzero.sdk.tracking.handling.IllegalCommandException;
import io.fluxzero.sdk.web.RedirectPolicy;
import io.fluxzero.sdk.web.WebRequest;
import io.fluxzero.sdk.web.WebRequestSettings;
import io.fluxzero.sdk.web.WebResponse;

import java.net.URI;
import java.time.Duration;

/** Configured REST endpoint and request policy. The command/query handlers perform the actual I/O. */
final class HomeAssistantEndpoint {
    static final WebRequestSettings REQUEST_SETTINGS = WebRequestSettings.builder()
            .timeout(Duration.ofSeconds(5)).redirectPolicy(RedirectPolicy.NEVER)
            .maxRetries(2).retryDelay(Duration.ofMillis(250)).build();
    final URI baseUrl;
    final String token;

    private HomeAssistantEndpoint(URI baseUrl, String token) {
        this.baseUrl = baseUrl;
        this.token = token;
    }

    static HomeAssistantEndpoint load(HomeAssistantId connectionId) {
        var connection = Fluxzero.loadModel(connectionId).get();
        if (connection == null) throw new IllegalCommandException("Connect Home Assistant to this home first.");
        String prefix = "home-assistant." + connection.details().configuration();
        String url = ApplicationProperties.getProperty(prefix + ".url");
        String token = ApplicationProperties.getProperty(prefix + ".token");
        if (url == null || token == null || !token.matches("[A-Za-z0-9._~+/-]+=*")) {
            throw new HomeAssistantUnavailable("Configure the Home Assistant URL and access token.");
        }
        URI base;
        try { base = URI.create(url); }
        catch (IllegalArgumentException invalid) { throw invalidUrl(); }
        if (!("http".equals(base.getScheme()) || "https".equals(base.getScheme())) || base.getHost() == null
                || base.getRawUserInfo() != null || base.getRawQuery() != null || base.getRawFragment() != null) {
            throw invalidUrl();
        }
        return new HomeAssistantEndpoint(URI.create(url.replaceAll("/+$", "") + "/"), token);
    }

    WebRequest get(String path) {
        return authorize(WebRequest.get(baseUrl.resolve(path).toString()));
    }

    WebRequest post(String path, Object body) {
        return authorize(WebRequest.post(baseUrl.resolve(path).toString()).contentType("application/json").body(body));
    }

    private WebRequest authorize(WebRequest.Builder request) {
        return request.header("Authorization", "Bearer " + token).header("Accept", "application/json").build();
    }

    static WebResponse requireSuccess(WebResponse response) {
        if (response.getStatus() == 401 || response.getStatus() == 403) {
            throw new HomeAssistantUnavailable("Home Assistant refused access. Check the configured token and permissions.");
        }
        if (response.getStatus() == 502 || response.getStatus() == 503 || response.getStatus() == 504) {
            throw new HomeAssistantUnavailable("Home Assistant is temporarily unavailable.");
        }
        if (response.getStatus() < 200 || response.getStatus() >= 300) {
            throw new HomeAssistantUnavailable("Home Assistant returned HTTP " + response.getStatus() + ".");
        }
        return response;
    }

    private static HomeAssistantUnavailable invalidUrl() {
        return new HomeAssistantUnavailable("Configure an absolute Home Assistant HTTP(S) base URL without credentials, query or fragment.");
    }
}
