package io.fluxzero.home.model;

/** Shared vocabulary for rejected home actions. */
public final class Rules {
    private Rules() {}
    public static void require(boolean condition, String explanation) {
        if (!condition) throw new HomeRuleViolation(explanation);
    }
}
