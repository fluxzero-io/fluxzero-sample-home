package io.fluxzero.home.homeassistant;

import io.fluxzero.sdk.configuration.ApplicationProperties;

import java.net.URI;

/** Resolves URL and credentials together from a trusted configuration group, outside domain messages. */
final class HomeAssistantAccess {
    final URI baseUrl;
    final String token;

    private HomeAssistantAccess(URI baseUrl, String token) {
        this.baseUrl = baseUrl;
        this.token = token;
    }

    static HomeAssistantAccess load(HomeAssistantConnection connection) {
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
        return new HomeAssistantAccess(URI.create(url.replaceAll("/+$", "") + "/"), token);
    }

    private static HomeAssistantUnavailable invalidUrl() {
        return new HomeAssistantUnavailable("Configure an absolute Home Assistant HTTP(S) base URL without credentials, query or fragment.");
    }
}
