package io.fluxzero.home.access;

import io.fluxzero.sdk.tracking.handling.authentication.User;

public record HomeUser(String id) implements User {
    public static final HomeUser SYSTEM = new HomeUser("$system");
    @Override public boolean hasRole(String role) { return equals(SYSTEM) && "SYSTEM".equals(role); }
}
