package io.fluxzero.home;

import io.fluxzero.sdk.tracking.handling.authentication.NoUserRequired;
import io.fluxzero.sdk.web.ServeStatic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.stereotype.Component;

/** Production assets; fz dev routes the UI to Vite during development. */
@Component
@ConditionalOnResource(resources = "classpath:/static/index.html")
@NoUserRequired
@ServeStatic(value = "/", resourcePath = "classpath:/static", ignorePaths = {
        "/api/*", "/app/*", "/login", "/oauth2/*", "/.well-known/*", "/userinfo"})
public class HomeFrontend {}
