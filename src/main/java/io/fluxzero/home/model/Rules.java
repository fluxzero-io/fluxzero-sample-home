package io.fluxzero.home.model;

/** Shared vocabulary for rejected home actions. */
public final class Rules {
    private Rules() {}
    public static void require(boolean condition, String explanation) {
        if (!condition) throw new HomeRuleViolation(explanation);
    }
    public static void named(String name) {
        require(name != null && !name.isBlank() && name.length() <= 120,
                "Give this a name between 1 and 120 characters.");
    }
}
