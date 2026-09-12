package io.fluxzero.home.model;

/** An understandable reason why a requested home action cannot be performed. */
public final class HomeRuleViolation extends io.fluxzero.sdk.common.exception.FunctionalException {
    public HomeRuleViolation(String message) { super(message); }
}
